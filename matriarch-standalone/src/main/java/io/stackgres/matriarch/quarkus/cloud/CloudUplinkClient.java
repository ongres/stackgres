package io.stackgres.matriarch.quarkus.cloud;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.stub.StreamObserver;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.matriarch.Matriarch;
import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.quarkus.api.ProtoMapper;
import io.stackgres.proto.api.v1.Environment;
import io.stackgres.proto.control.v1.*;
import io.stackgres.proto.types.v1.Id;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The matriarch-side end of the {@code control.v1} link for the standalone (bare-metal) matriarch:
 * dials OUT to stackgres-cloud, registers this environment, and streams its state up — the mirror of
 * how {@code slon}/{@code slony} dial up to this matriarch, one level higher. Opt-in by setting
 * {@code STACKGRES_ENDPOINT_URL} (the cloud endpoint); authenticates with the {@code STACKGRES_TOKEN}
 * JWT (env credential, P10) — the same two env vars the CLI uses. Unlike the operator (which derives the
 * environment from the StackGres installation id), the standalone takes its {@code environment_id}/name/
 * kind from {@code stackgres.cloud.environment.*} config (defaults to a single {@code local} bare-metal
 * environment). Read-only for v1.
 *
 * <p>All sends run on a single "actor" thread so the (non-thread-safe) request {@link StreamObserver}
 * is touched from one place and the sequence numbering stays monotonic: domain {@link ClusterEvent}s
 * (observed on CDI threads) and downstream messages (on gRPC threads) are both posted onto it. On
 * (re)connect it registers, then — when the cloud asks — sends a full snapshot built from the core's
 * current clusters (via the standalone {@link ProtoMapper}); subsequent events ship as
 * sequence-numbered deltas. A dropped stream reconnects with capped backoff and re-anchors.
 */
@ApplicationScoped
public class CloudUplinkClient {

    private static final Logger LOG = Logger.getLogger(CloudUplinkClient.class);
    private static final String AGENT = "stackgres-matriarch-uplink";
    private static final long MAX_BACKOFF_MS = 30_000;

    @Inject
    Matriarch matriarch;

    // This matriarch's stable, unique environment identity (config or generated-and-persisted) — the
    // SAME id the api.v1 server stamps on its clusters, so the cloud and a direct CLI see one identity.
    @Inject
    io.stackgres.matriarch.quarkus.identity.EnvironmentIdentity identity;

    @ConfigProperty(name = "stackgres.cloud.heartbeat-interval")
    Duration heartbeatInterval;

    // The cloud endpoint this matriarch dials out to, and its JWT credential — the SAME env vars every
    // component uses (unified): STACKGRES_ENDPOINT_URL / STACKGRES_TOKEN. Empty URL = uplink disabled.
    // Read by their exact env-var names (the environment is an MP Config source).
    @ConfigProperty(name = "STACKGRES_ENDPOINT_URL")
    Optional<String> endpointUrl;

    @ConfigProperty(name = "STACKGRES_TOKEN")
    Optional<String> token;

    // Transport selection. Default is TLS with the JVM's trust store. `plaintext` is for an h2c
    // endpoint (local dev / a plaintext ingress); `insecure-tls` keeps TLS but trusts any server
    // cert — the client-side mirror of the cloud's own quarkus.oidc.tls.verification=none, needed
    // when the cloud presents a self-signed cert.
    @ConfigProperty(name = "stackgres.cloud.plaintext")
    Optional<Boolean> plaintext;

    @ConfigProperty(name = "stackgres.cloud.insecure-tls")
    Optional<Boolean> insecureTls;

    private boolean enabled = false;
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

    /**
     * Begin dialing out (idempotent no-op when disabled/misconfigured). Public for host/test wiring.
     */
    public void start() {
        if (endpointUrl.isEmpty()) {
            LOG.debug("STACKGRES_ENDPOINT_URL is not set -> cloud uplink disabled");
            enabled = false;
            return;
        }
        enabled = true;
        environmentId = identity.id();
        actor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cloud-uplink");
            t.setDaemon(true);
            return t;
        });
        LOG.infof("cloud uplink starting: env=%s -> %s [token=%s]", environmentId, endpointUrl, token.isEmpty() ? "none" : "set");
        actor.execute(this::connect);
        long everyMs = heartbeatInterval.toMillis();
        actor.scheduleAtFixedRate(this::sendHeartbeat, everyMs, everyMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (actor != null) {
            actor.shutdownNow();
        }
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    // ---- everything below runs on the single actor thread ----

    private void connect() {
        try {
            if (channel == null || channel.isShutdown()) {
                channel = buildChannel(endpointUrl.get());
            }
            snapshotAnchored = false;
            up = UplinkGrpc.newStub(channel).connect(new DownstreamHandler());
            up.onNext(MatriarchMessage.newBuilder()
                    .setRegistration(Registration.newBuilder()
                            .setEnvironment(environmentProto())
                            .setAgentVersion(AGENT)
                            .setResumeFromSeq(0))   // PoC: always re-anchor on a fresh snapshot
                    .build());
            // The stream is now open; whether the cloud accepts it surfaces asynchronously in the
            // DownstreamHandler (ACK) or, on failure, as a gRPC Status in onError.
            LOG.infof("cloud uplink connecting: env=%s -> %s", environmentId, endpointUrl.get());
        } catch (Exception e) {
            LOG.warnf("cloud uplink connect failed: %s — retrying in %dms", e.getMessage(), backoffMs);
            scheduleReconnect();
        }
    }

    /**
     * Build the outbound channel for the configured transport: plaintext h2c, TLS trusting any cert
     * (insecure-tls, for a self-signed cloud), or — the default — TLS validated against the JVM trust
     * store.
     */
    private ManagedChannel buildChannel(String target) throws javax.net.ssl.SSLException {
        if (plaintext.orElse(false)) {
            ManagedChannelBuilder<?> b = ManagedChannelBuilder.forTarget(target).usePlaintext();
            token.ifPresent(s -> b.intercept(new BearerTokenInterceptor(s)));
            return b.build();
        }
        NettyChannelBuilder b = NettyChannelBuilder.forTarget(target);
        if (insecureTls.orElse(false)) {
            b.sslContext(GrpcSslContexts.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build());
        }
        token.ifPresent(s -> b.intercept(new BearerTokenInterceptor(s)));
        return b.build();
    }

    private void sendSnapshot() {
        if (up == null) {
            return;
        }
        StateSnapshot.Builder snap = StateSnapshot.newBuilder().setSeq(seq);
        for (Cluster c : matriarch.listClusters()) {
            snap.addCluster(toProto(c));
        }
        up.onNext(MatriarchMessage.newBuilder().setSnapshot(snap).build());
        snapshotAnchored = true;
        LOG.debugf("cloud uplink snapshot sent: seq=%d clusters=%d", seq, snap.getClusterCount());
    }

    void onClusterEvent(@Observes ClusterEvent event) {
        ScheduledExecutorService a = actor;
        if (!enabled || a == null) {
            return;
        }
        a.execute(() -> forward(event));
    }

    private void forward(ClusterEvent event) {
        long s = ++seq;
        if (up == null || !snapshotAnchored) {
            return;   // not connected/anchored yet; the next reconnect snapshot carries current state
        }
        SeqEvent.Builder ev = SeqEvent.newBuilder()
                .setSeq(s)
                .setEvent(ProtoMapper.toProtoEvent(event));
        if (event instanceof ClusterEvent.ClusterDeleted) {
            ev.setRemoved(Id.newBuilder().setValue(event.clusterId().value()));
        } else {
            Cluster c = findCluster(event.clusterId());
            if (c != null) {
                ev.setUpserted(toProto(c));
            }
        }
        try {
            up.onNext(MatriarchMessage.newBuilder().setEvent(ev).build());
        } catch (RuntimeException e) {
            LOG.debugf("event send failed: %s", e.getMessage());
        }
    }

    /**
     * Map a domain cluster to api.v1 and stamp this environment's id (the standalone mapper omits it).
     */
    private io.stackgres.proto.api.v1.Cluster toProto(Cluster c) {
        return ProtoMapper.toProto(c).toBuilder().setEnvironmentId(environmentId).build();
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
            case PAYLOAD_NOT_SET -> {
            }
        }
    }

    private void scheduleReconnect() {
        up = null;
        long delay = backoffMs;
        backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
        if (actor != null && !actor.isShutdown()) {
            actor.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
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

    private Environment environmentProto() {
        return identity.environment();
    }

    private final class DownstreamHandler implements StreamObserver<CloudMessage> {
        @Override
        public void onNext(CloudMessage m) {
            actor.execute(() -> handleDown(m));
        }

        @Override
        public void onError(Throwable t) {
            Status s = Status.fromThrowable(t);
            LOG.warnf("cloud uplink stream error: %s: %s", s.getCode(), s.getDescription());
            actor.execute(CloudUplinkClient.this::scheduleReconnect);
        }

        @Override
        public void onCompleted() {
            LOG.debug("cloud uplink stream completed by server");
            actor.execute(CloudUplinkClient.this::scheduleReconnect);
        }
    }

}