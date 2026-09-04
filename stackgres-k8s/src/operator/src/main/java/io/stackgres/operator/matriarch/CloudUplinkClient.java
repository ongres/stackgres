/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.matriarch.Matriarch;
import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.operator.app.OperatorInstallationInfoHolder;
import io.stackgres.proto.api.v1.Environment;
import io.stackgres.proto.control.v1.CloudMessage;
import io.stackgres.proto.control.v1.ControlResponse;
import io.stackgres.proto.control.v1.Heartbeat;
import io.stackgres.proto.control.v1.MatriarchMessage;
import io.stackgres.proto.control.v1.Registration;
import io.stackgres.proto.control.v1.SeqEvent;
import io.stackgres.proto.control.v1.StateSnapshot;
import io.stackgres.proto.control.v1.UplinkGrpc;
import io.stackgres.proto.types.v1.Id;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * The matriarch-side end of the {@code control.v1} link: dials OUT to stackgres-cloud, registers this
 * StackGres install as an environment, and streams its state up — the mirror of how {@code slon} dials
 * up to a matriarch, one level higher. Opt-in via {@code stackgres.cloud.enabled}; authenticates with
 * the {@code STACKGRES_TOKEN} JWT (env credential, P10). The api.v1 {@code environment_id} is the
 * StackGres installation id. Read-only for v1: it pushes snapshots/events and answers resync, and
 * ignores any control coming down.
 *
 * <p>All sends run on a single "actor" thread so the (non-thread-safe) request {@link StreamObserver}
 * is only ever touched from one place and the sequence numbering stays monotonic: domain
 * {@link ClusterEvent}s (observed on CDI threads) and downstream messages (on gRPC threads) are both
 * posted onto it. On (re)connect it registers, then — when the cloud asks — sends a full snapshot built
 * from the core's current clusters (via {@link ClusterProtoMapper}); subsequent events ship as
 * sequence-numbered deltas. A dropped stream reconnects with capped backoff and re-anchors on a fresh
 * snapshot.
 */
@ApplicationScoped
public class CloudUplinkClient {

  private static final Logger LOG = Logger.getLogger(CloudUplinkClient.class);
  private static final String AGENT = "stackgres-matriarch-uplink";
  private static final long MAX_BACKOFF_MS = 30_000;

  @Inject
  Matriarch matriarch;

  @Inject
  OperatorInstallationInfoHolder installationInfoHolder;

  @ConfigProperty(name = "stackgres.cloud.enabled", defaultValue = "false")
  boolean enabled;

  @ConfigProperty(name = "stackgres.cloud.plaintext", defaultValue = "true")
  boolean plaintext;

  @ConfigProperty(name = "stackgres.cloud.heartbeat-interval", defaultValue = "5s")
  Duration heartbeatInterval;

  @Inject
  Config config;

  // The cloud endpoint (STACKGRES_ENDPOINT_URL) and JWT credential (STACKGRES_TOKEN) — the same env
  // vars the CLI and standalone matriarch use — resolved lazily in start() (only when the uplink is
  // enabled) via Config.getOptionalValue. An empty-string @ConfigProperty default on a String fails
  // eager injection and aborts operator startup, so we must NOT inject these directly.
  // Package-private so tests can set them.
  String url;
  String token;

  // --- actor-thread state (only touched on `actor`) ---
  private volatile boolean stopping = false;
  private ScheduledExecutorService actor;
  private ManagedChannel channel;
  private StreamObserver<MatriarchMessage> up;
  private long seq;
  private boolean snapshotAnchored;
  private long backoffMs = 1_000;
  private String environmentId;

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  /** Begin dialing out (idempotent no-op when disabled/misconfigured). Public for host/test wiring. */
  public void start() {
    if (!enabled) {
      LOG.debug("cloud uplink disabled (stackgres.cloud.enabled=false)");
      return;
    }
    if (url == null) {
      url = config.getOptionalValue("STACKGRES_ENDPOINT_URL", String.class).orElse("");
    }
    if (token == null) {
      token = config.getOptionalValue("STACKGRES_TOKEN", String.class).orElse("");
    }
    if (url.isBlank()) {
      LOG.warn("stackgres.cloud.enabled=true but STACKGRES_ENDPOINT_URL is empty — uplink not started");
      return;
    }
    environmentId = resolveEnvironmentId();
    actor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "cloud-uplink");
      t.setDaemon(true);
      return t;
    });
    LOG.infof("cloud uplink starting: env=%s -> %s [token=%s]",
        environmentId, url, token.isBlank() ? "none" : "set");
    actor.execute(this::connect);
    long everyMs = heartbeatInterval.toMillis();
    actor.scheduleAtFixedRate(this::sendHeartbeat, everyMs, everyMs, TimeUnit.MILLISECONDS);
  }

  public void stop() {
    // Flag first so gRPC callbacks (onError/onNext) fired by the teardown below skip re-dispatching to
    // the actor, then drop the channel BEFORE the actor — otherwise the channel's shutdown fires
    // onError, which tries to hand a reconnect to an already-dead actor (RejectedExecutionException).
    stopping = true;
    if (channel != null) {
      channel.shutdownNow();
    }
    if (actor != null) {
      actor.shutdownNow();
    }
  }

  /**
   * Hand a task to the single actor thread from a gRPC callback (or CDI) thread, silently skipping it
   * while shutting down: a stream error/close during {@link #stop()} can fire onError/onNext after the
   * actor is gone, and an unguarded {@code execute} would throw RejectedExecutionException from inside
   * gRPC's onClose. The try/catch covers the shutdown racing the check.
   */
  private void runOnActor(Runnable task) {
    ScheduledExecutorService a = actor;
    if (stopping || a == null || a.isShutdown()) {
      return;
    }
    try {
      a.execute(task);
    } catch (java.util.concurrent.RejectedExecutionException ignore) {
      // actor shut down between the check and submit — nothing to do on the way down
    }
  }

  // ---- everything below runs on the single actor thread ----

  private void connect() {
    try {
      if (channel == null || channel.isShutdown()) {
        ManagedChannelBuilder<?> b = ManagedChannelBuilder.forTarget(url);
        if (plaintext) {
          b.usePlaintext();
        }
        if (!token.isBlank()) {
          b.intercept(new BearerTokenInterceptor(token));
        }
        channel = b.build();
      }
      snapshotAnchored = false;
      up = UplinkGrpc.newStub(channel).connect(new DownstreamHandler());
      up.onNext(MatriarchMessage.newBuilder()
          .setRegistration(Registration.newBuilder()
              .setEnvironment(environmentProto())
              .setAgentVersion(AGENT)
              .setResumeFromSeq(0))   // PoC: always re-anchor on a fresh snapshot
          .build());
      LOG.infof("cloud uplink registered env=%s", environmentId);
    } catch (RuntimeException e) {
      LOG.warnf("cloud uplink connect failed: %s — retrying in %dms", e.getMessage(), backoffMs);
      scheduleReconnect();
    }
  }

  private void sendSnapshot() {
    if (up == null) {
      return;
    }
    StateSnapshot.Builder snap = StateSnapshot.newBuilder().setSeq(seq);
    for (Cluster c : matriarch.listClusters()) {
      snap.addCluster(ClusterProtoMapper.toProto(c, environmentId));
    }
    up.onNext(MatriarchMessage.newBuilder().setSnapshot(snap).build());
    snapshotAnchored = true;
    LOG.debugf("cloud uplink snapshot sent: seq=%d clusters=%d", seq, snap.getClusterCount());
  }

  void onClusterEvent(@Observes ClusterEvent event) {
    if (!enabled) {
      return;
    }
    runOnActor(() -> forward(event));
  }

  private void forward(ClusterEvent event) {
    long s = ++seq;
    if (up == null || !snapshotAnchored) {
      return;   // not connected/anchored yet; the next reconnect snapshot carries current state
    }
    SeqEvent.Builder ev = SeqEvent.newBuilder()
        .setSeq(s)
        .setEvent(ClusterProtoMapper.toProtoEvent(event));
    if (event instanceof ClusterEvent.ClusterDeleted) {
      ev.setRemoved(Id.newBuilder().setValue(event.clusterId().value()));
    } else {
      Cluster c = findCluster(event.clusterId());
      if (c != null) {
        ev.setUpserted(ClusterProtoMapper.toProto(c, environmentId));
      }
    }
    try {
      up.onNext(MatriarchMessage.newBuilder().setEvent(ev).build());
    } catch (RuntimeException e) {
      LOG.debugf("event send failed: %s", e.getMessage());
    }
  }

  private void sendHeartbeat() {
    if (up == null) {
      return;
    }
    try {
      up.onNext(MatriarchMessage.newBuilder()
          .setHeartbeat(Heartbeat.newBuilder().setLastSeq(seq))
          .build());
    } catch (RuntimeException e) {
      LOG.debugf("heartbeat failed: %s", e.getMessage());
    }
  }

  private void handleDown(CloudMessage m) {
    switch (m.getPayloadCase()) {
      case ACK -> {
        backoffMs = 1_000;   // proven-working connection
        if (m.getAck().getSnapshotRequired()) {
          sendSnapshot();
        }
      }
      case RESYNC -> {
        LOG.infof("cloud requested resync: %s", m.getResync().getReason());
        sendSnapshot();
      }
      case CONTROL -> {
        // The operator is read-only for now: it observes k8s but cannot execute user writes. Reject
        // cleanly so the cloud fails the api.v1 call instead of hanging. FAILED_PRECONDITION (not
        // UNIMPLEMENTED): the write IS supported over the cloud — this particular environment just
        // can't satisfy it — so the CLI surfaces this reason rather than "not available over the cloud".
        up.onNext(MatriarchMessage.newBuilder().setControlResponse(ControlResponse.newBuilder()
            .setRequestId(m.getControl().getRequestId())
            .setError(com.google.rpc.Status.newBuilder()
                .setCode(io.grpc.Status.Code.FAILED_PRECONDITION.value())
                .setMessage("this Kubernetes environment is read-only from the cloud — manage its "
                    + "clusters with the StackGres operator (SGCluster resources); cloud-driven writes "
                    + "aren't supported yet")))
            .build());
      }
      case HEARTBEAT_ACK -> {
        // Keep-alive reply to our heartbeat — a frame on the cloud->local direction (proxy idle
        // guard). Nothing to do.
      }
      case PAYLOAD_NOT_SET -> { }
      default -> { }
    }
  }

  private void scheduleReconnect() {
    up = null;
    long delay = backoffMs;
    backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
    if (stopping || actor == null || actor.isShutdown()) {
      return;
    }
    try {
      actor.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
    } catch (java.util.concurrent.RejectedExecutionException ignore) {
      // shutting down — no reconnect needed
    }
  }

  private Cluster findCluster(ClusterId id) {
    for (Cluster c : matriarch.listClusters()) {
      if (c.spec().id().value().equals(id.value())) {
        return c;
      }
    }
    return null;
  }

  private String resolveEnvironmentId() {
    String id = installationInfoHolder.getInstallationId();
    return id == null || id.isBlank() ? "local" : id;
  }

  private Environment environmentProto() {
    return Environment.newBuilder()
        .setId(Id.newBuilder().setValue(environmentId))
        .setKind(Environment.Kind.KIND_K8S_STACKGRES)
        .build();
  }

  private final class DownstreamHandler implements StreamObserver<CloudMessage> {
    @Override
    public void onNext(CloudMessage m) {
      runOnActor(() -> handleDown(m));
    }

    @Override
    public void onError(Throwable t) {
      LOG.debugf("cloud uplink stream error: %s", t.getMessage());
      runOnActor(CloudUplinkClient.this::scheduleReconnect);
    }

    @Override
    public void onCompleted() {
      LOG.debug("cloud uplink stream completed by server");
      runOnActor(CloudUplinkClient.this::scheduleReconnect);
    }
  }
}
