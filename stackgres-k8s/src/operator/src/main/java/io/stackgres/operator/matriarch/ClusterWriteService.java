/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceWriter;
import io.stackgres.common.resource.DbOpsFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.matriarch.Defaults;
import io.stackgres.matriarch.Matriarch;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.status.RunStatus;
import io.stackgres.operator.app.OperatorInstallationInfoHolder;
import io.stackgres.proto.api.v1.ClusterOperationProgress;
import io.stackgres.proto.api.v1.ClusterSelector;
import io.stackgres.proto.api.v1.CreateClusterRequest;
import io.stackgres.proto.api.v1.DeleteClusterRequest;
import io.stackgres.proto.api.v1.GetClusterCredentialsRequest;
import io.stackgres.proto.api.v1.GetClusterCredentialsResponse;
import io.stackgres.proto.api.v1.OperationStatus;
import io.stackgres.proto.api.v1.RestartClusterRequest;
import io.stackgres.proto.api.v1.StartClusterRequest;
import io.stackgres.proto.api.v1.StopClusterRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;

/**
 * The api.v1 WRITE slice for the K8s embed. Unlike the standalone matriarch it does NOT route through
 * {@code Matriarch.createCluster}/its StateStore — the operator + {@code SGCluster} CRs in etcd stay the
 * single source of truth. Each write just translates the request into an {@code SGCluster} CR operation
 * via the shared {@link CustomResourceWriter}; the operator's admission mutators/validators and reconciler
 * own everything downstream, and the created CR flows back into the read model through
 * {@link StackGresObserver}.
 *
 * <p>Every lifecycle op is accepted-then-watch (§3.6): create/delete stream {@link ClusterOperationProgress}
 * and drive the watch off the observer's status (correlated by the CR's uid = domain {@link ClusterId}),
 * on the {@link ManagedExecutor} so the gRPC thread never blocks. A validation/conflict rejection surfaces
 * synchronously as a gRPC error (the op was never accepted); a runtime provisioning stall ends the watch at
 * the deadline as FAILED (enriching the observed status with a real FAILED signal is a fast-follow).
 */
@ApplicationScoped
public class ClusterWriteService {

  // Interval for the per-operation watch loop's in-memory status re-read (create/delete). Not an API
  // call and only runs while an operation is in flight, so a tight interval just tightens completion
  // latency (≤ this) at negligible cost.
  private static final long POLL_MILLIS = 500;
  private static final Duration CREATE_TIMEOUT = Duration.ofMinutes(5);
  private static final Duration DELETE_TIMEOUT = Duration.ofMinutes(2);
  private static final Duration RESTART_TIMEOUT = Duration.ofMinutes(10);
  private static final Duration POD_CHECK_INTERVAL = Duration.ofSeconds(5);

  @Inject
  Matriarch matriarch;

  @Inject
  CustomResourceWriter<StackGresCluster> clusterWriter;

  @Inject
  CustomResourceScanner<StackGresCluster> clusterScanner;

  @Inject
  SecretFinder secretFinder;

  @Inject
  ResourceScanner<Pod> podScanner;

  @Inject
  LabelFactoryForCluster labelFactory;

  @Inject
  CustomResourceWriter<StackGresDbOps> dbOpsWriter;

  @Inject
  DbOpsFinder dbOpsFinder;

  @Inject
  ManagedExecutor executor;

  @Inject
  OperatorInstallationInfoHolder installationInfoHolder;

  // Namespace a created cluster lands in: the request's placement["namespace"] hint, else this default.
  @ConfigProperty(name = "matriarch.cluster.default-namespace", defaultValue = "default")
  String defaultNamespace;

  // PV size the api.v1 create request does not carry (§ mapper) — defaulted here, tunable per install.
  @ConfigProperty(name = "matriarch.cluster.default-storage-size", defaultValue = "1Gi")
  String defaultStorageSize;

  // Grace before a stuck pod (image pull, crash loop, unschedulable) is reported FAILED — same value the
  // observer uses; long enough that a normal, slow-but-succeeding start is never mislabeled.
  @ConfigProperty(name = "matriarch.cluster.failure-grace", defaultValue = "60s")
  Duration failureGrace;

  // ---- create ----

  public void create(CreateClusterRequest request, StreamObserver<ClusterOperationProgress> obs) {
    // Fully async: the ONLY thing that waits is the gRPC stream itself (the CLI spinner). Both blocking
    // K8s steps run on a worker thread, never the gRPC/uplink caller thread — (1) the CR create, a quick
    // POST that merely persists desired state (the operator's own reconcile loop brings pods up; we do NOT
    // wait for that here), and (2) the observe-to-healthy watch.
    CreateClusterRequest req = withDefaultName(request);
    executor.execute(() -> runCreate(req, obs));
  }

  private void runCreate(CreateClusterRequest req, StreamObserver<ClusterOperationProgress> obs) {
    String namespace = namespaceFor(req);
    StackGresCluster created;
    try {
      // Hits the API server, so admission mutators default the minimal CR and validators reject a bad one
      // synchronously; returns the object with its server-assigned uid = our ClusterId (zero-race correlation).
      created = clusterWriter.create(ClusterWriteMapper.toCluster(req, namespace, defaultStorageSize));
    } catch (KubernetesClientException e) {
      // The create POST only persists the CR — it does not wait for pods. If the API server was so slow the
      // client timed out, the object may still have been written; recover it idempotently by name so a
      // slow-but-successful create becomes ACCEPTED, not a spurious failure. A genuine failure (nothing
      // created, or the API server still unreachable) falls through to the mapped gRPC error.
      created = existingByName(req.getName(), namespace).orElse(null);
      if (created == null) {
        obs.onError(toGrpcError(e));   // 409 -> ALREADY_EXISTS, 422/400 -> INVALID_ARGUMENT, ...
        return;
      }
    }
    runCreateWatch(created, obs, environmentId());
  }

  /**
   * The default cluster name for a blank request, minted the SAME way the standalone core does
   * ({@link Defaults#nextName}) so naming is identical across environments.
   *
   * <p>TEMPORARY: remove once K8s writes route through the core ({@code Matriarch.planDesiredSpec} already
   * fills blank fields). We bypass the core here, so nothing applies it — and K8s requires metadata.name.
   */
  private CreateClusterRequest withDefaultName(CreateClusterRequest request) {
    if (request.getName() != null && !request.getName().isBlank()) {
      return request;
    }
    return request.toBuilder()
        .setName(Defaults.nextName(matriarch.listClusters().stream()
            .map(c -> c.spec().name()).collect(Collectors.toSet())))
        .build();
  }

  /** An already-created CR by name (idempotent recovery after a create timeout); empty on any error. */
  private Optional<StackGresCluster> existingByName(String name, String namespace) {
    try {
      return clusterScanner.getResources(namespace).stream()
          .filter(cr -> name.equals(cr.getMetadata().getName()))
          .findFirst();
    } catch (RuntimeException e) {
      return Optional.empty();   // API server still unreachable — treat as unrecoverable
    }
  }

  private void runCreateWatch(StackGresCluster cr, StreamObserver<ClusterOperationProgress> obs, String envId) {
    ClusterId id = new ClusterId(cr.getMetadata().getUid());
    Cluster accepted = StackGresMapper.toCluster(cr, null);
    obs.onNext(frame(OperationStatus.OPERATION_STATUS_ACCEPTED, accepted, envId));
    long start = System.nanoTime();
    long deadline = start + CREATE_TIMEOUT.toNanos();
    long nextPodCheck = start + failureGrace.toNanos();   // first pod check only after the grace window
    Cluster last = accepted;
    RunStatus lastStatus = null;
    try {
      while (true) {
        Cluster c = matriarch.getCluster(id);   // fed by StackGresObserver; may be null for a tick or two
        if (c != null) {
          last = c;
          RunStatus s = c.status().runStatus();
          if (s == RunStatus.HEALTHY) {
            obs.onNext(frame(OperationStatus.OPERATION_STATUS_SUCCEEDED, c, envId));
            obs.onCompleted();
            return;
          }
          if (s != lastStatus && s != RunStatus.FAILED) {   // FAILED is decided by the pod check below (with a reason)
            obs.onNext(frame(OperationStatus.OPERATION_STATUS_RUNNING, c, envId));
            lastStatus = s;
          }
        }
        long now = System.nanoTime();
        if (now >= nextPodCheck) {   // throttled; the classifier only fires once a pod is past the grace window
          nextPodCheck = now + POD_CHECK_INTERVAL.toNanos();
          Optional<String> failure = podFailureReason(cr);
          if (failure.isPresent()) {   // stuck pod (image pull / crash loop / unschedulable) — fail with why
            obs.onNext(failedFrame(last, Status.Code.FAILED_PRECONDITION.value(),
                "cluster failed to start: " + failure.get(), envId));
            obs.onCompleted();
            return;
          }
        }
        if (now > deadline) {
          obs.onNext(failedFrame(last, 4, "timed out waiting for cluster to become healthy", envId));
          obs.onCompleted();
          return;
        }
        Thread.sleep(POLL_MILLIS);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      obs.onNext(failedFrame(last, 1, "watch interrupted", envId));
      obs.onCompleted();
    }
  }

  /** A persisted pod-failure reason for the cluster (image pull / crash loop / unschedulable), or empty. */
  private Optional<String> podFailureReason(StackGresCluster cr) {
    try {
      List<Pod> pods = podScanner.getResourcesInNamespaceWithLabels(
          cr.getMetadata().getNamespace(), labelFactory.clusterLabels(cr));
      return PodFailure.detect(pods, Instant.now(), failureGrace);
    } catch (RuntimeException e) {
      return Optional.empty();   // transient scan failure — don't fail the op on our own inability to check
    }
  }

  // ---- delete ----

  public void delete(DeleteClusterRequest req, StreamObserver<ClusterOperationProgress> obs) {
    Cluster target = resolveTarget(req.getSelector(), obs);
    if (target == null) {
      return;   // resolveTarget already sent the gRPC error
    }
    ClusterId id = target.spec().id();
    String name = target.spec().name();
    String namespace = target.spec().tags().get("namespace");
    executor.execute(() -> runDeleteWatch(id, name, namespace, target, obs, environmentId()));
  }

  /**
   * Resolve a lifecycle selector to a live cluster (by id or name). Sends the matching gRPC error and
   * returns null on an unsupported selector, a miss, or a cluster with no known namespace.
   */
  private Cluster resolveTarget(ClusterSelector sel, StreamObserver<?> obs) {
    Cluster target;
    switch (sel.getMatchCase()) {
      case ID -> target = matriarch.getCluster(new ClusterId(sel.getId().getValue()));
      case NAME -> target = byName(sel.getName());
      default -> {
        obs.onError(Status.UNIMPLEMENTED
            .withDescription("only id or name selectors are supported").asRuntimeException());
        return null;
      }
    }
    if (target == null) {
      obs.onError(Status.NOT_FOUND.withDescription("no such cluster").asRuntimeException());
      return null;
    }
    String namespace = target.spec().tags().get("namespace");
    if (namespace == null || namespace.isBlank()) {
      obs.onError(Status.FAILED_PRECONDITION
          .withDescription("cluster namespace unknown").asRuntimeException());
      return null;
    }
    return target;
  }

  private void runDeleteWatch(ClusterId id, String name, String namespace, Cluster target,
      StreamObserver<ClusterOperationProgress> obs, String envId) {
    obs.onNext(frame(OperationStatus.OPERATION_STATUS_ACCEPTED, target, envId));
    try {
      clusterWriter.delete(ClusterWriteMapper.ref(name, namespace));
    } catch (KubernetesClientException e) {
      if (e.getCode() != 404) {   // 404 = already gone -> fall through to success
        obs.onNext(failedFrame(target, 13, "delete failed: " + e.getMessage(), envId));
        obs.onCompleted();
        return;
      }
    }
    long deadline = System.nanoTime() + DELETE_TIMEOUT.toNanos();
    try {
      while (matriarch.getCluster(id) != null) {   // gone from the observer = teardown accepted
        if (System.nanoTime() > deadline) {
          obs.onNext(failedFrame(target, 4, "timed out waiting for cluster teardown", envId));
          obs.onCompleted();
          return;
        }
        Thread.sleep(POLL_MILLIS);
      }
      obs.onNext(frame(OperationStatus.OPERATION_STATUS_SUCCEEDED, target, envId));
      obs.onCompleted();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      obs.onNext(failedFrame(target, 1, "watch interrupted", envId));
      obs.onCompleted();
    }
  }

  // ---- restart (SGDbOps) / start / stop ----

  public void restart(RestartClusterRequest req, StreamObserver<ClusterOperationProgress> obs) {
    Cluster target = resolveTarget(req.getSelector(), obs);
    if (target == null) {
      return;
    }
    String name = target.spec().name();
    String namespace = target.spec().tags().get("namespace");
    // Retry-safe: a valid, key-derived SGDbOps name so a resent restart attaches to the running op (409)
    // instead of stacking a second rolling restart; a bare idempotency key (often a UUID) isn't a legal
    // Kubernetes name, so the mapper prefixes/sanitizes it.
    String opName = ClusterWriteMapper.restartOpName(name, req.getIdempotencyKey());
    executor.execute(() -> runRestart(name, namespace, opName, target, obs, environmentId()));
  }

  private void runRestart(String clusterName, String namespace, String opName, Cluster target,
      StreamObserver<ClusterOperationProgress> obs, String envId) {
    obs.onNext(frame(OperationStatus.OPERATION_STATUS_ACCEPTED, target, envId));
    try {
      dbOpsWriter.create(ClusterWriteMapper.restartDbOps(opName, namespace, clusterName));
    } catch (KubernetesClientException e) {
      if (e.getCode() != 409) {   // 409 = a resend with the same key -> attach to the existing op
        obs.onNext(failedFrame(target, Status.Code.FAILED_PRECONDITION.value(),
            "restart failed: " + e.getMessage(), envId));
        obs.onCompleted();
        return;
      }
    }
    long deadline = System.nanoTime() + RESTART_TIMEOUT.toNanos();
    try {
      while (true) {
        for (Condition c : dbOpsConditions(namespace, opName)) {
          if ("Completed".equals(c.getType()) && "True".equals(c.getStatus())) {
            Cluster fresh = matriarch.getCluster(target.spec().id());
            obs.onNext(frame(OperationStatus.OPERATION_STATUS_SUCCEEDED, fresh != null ? fresh : target, envId));
            obs.onCompleted();
            return;
          }
          if ("Failed".equals(c.getType()) && "True".equals(c.getStatus())) {
            obs.onNext(failedFrame(target, Status.Code.FAILED_PRECONDITION.value(),
                "restart failed: " + conditionDetail(c), envId));
            obs.onCompleted();
            return;
          }
        }
        if (System.nanoTime() > deadline) {
          obs.onNext(failedFrame(target, 4, "timed out waiting for restart to complete", envId));
          obs.onCompleted();
          return;
        }
        Thread.sleep(POLL_MILLIS);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      obs.onNext(failedFrame(target, 1, "watch interrupted", envId));
      obs.onCompleted();
    }
  }

  /** The SGDbOps status conditions (empty on an absent op / transient scan error). */
  private List<Condition> dbOpsConditions(String namespace, String opName) {
    try {
      StackGresDbOps op = dbOpsFinder.findByNameAndNamespace(opName, namespace).orElse(null);
      if (op == null || op.getStatus() == null || op.getStatus().getConditions() == null) {
        return List.of();
      }
      return op.getStatus().getConditions();
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  private static String conditionDetail(Condition c) {
    if (c.getMessage() != null && !c.getMessage().isBlank()) {
      return c.getMessage();
    }
    return c.getReason() != null ? c.getReason() : "operation failed";
  }

  // Start/stop have no native StackGres mechanism (there is no cluster "stop"; the only lever is the
  // SGCluster instance count). Reject cleanly until that is designed, rather than fake it via scale-to-zero.
  public void start(StartClusterRequest req, StreamObserver<ClusterOperationProgress> obs) {
    obs.onError(Status.FAILED_PRECONDITION.withDescription(
        "start is not supported for Kubernetes environments yet — scale the cluster via its SGCluster instances")
        .asRuntimeException());
  }

  public void stop(StopClusterRequest req, StreamObserver<ClusterOperationProgress> obs) {
    obs.onError(Status.FAILED_PRECONDITION.withDescription(
        "stop is not supported for Kubernetes environments yet — scale the cluster via its SGCluster instances")
        .asRuntimeException());
  }

  // ---- credentials ----

  public void credentials(GetClusterCredentialsRequest req,
      StreamObserver<GetClusterCredentialsResponse> obs) {
    // Offload to a worker: the SecretFinder lookup below is a BLOCKING fabric8 GET, and this unary handler
    // otherwise runs on the gRPC vert.x event-loop thread — blocking it there deadlocks vert.x (the GET's
    // own HTTP response is processed by the very loop it's blocking) and, worse, starves the operator's
    // shared event loop so cluster reconciliation stalls for seconds. Run it off the loop instead.
    executor.execute(() -> runCredentials(req, obs));
  }

  private void runCredentials(GetClusterCredentialsRequest req,
      StreamObserver<GetClusterCredentialsResponse> obs) {
    Cluster c = matriarch.getCluster(new ClusterId(req.getClusterId().getValue()));
    if (c == null) {
      obs.onError(Status.NOT_FOUND.withDescription("no such cluster").asRuntimeException());
      return;
    }
    String namespace = c.spec().tags().get("namespace");
    if (namespace == null || namespace.isBlank()) {
      obs.onError(Status.FAILED_PRECONDITION
          .withDescription("cluster namespace unknown").asRuntimeException());
      return;
    }
    // StackGres holds the superuser credential in a Secret named after the cluster (PatroniUtil.secretName;
    // a valid cluster name passes through resourceName unchanged, so secret name == cluster name).
    Optional<Secret> secret = secretFinder.findByNameAndNamespace(c.spec().name(), namespace);
    if (secret.isEmpty() || secret.get().getData() == null) {
      obs.onError(Status.NOT_FOUND
          .withDescription("credentials secret not found for cluster " + c.spec().name())
          .asRuntimeException());
      return;
    }
    Map<String, String> data = secret.get().getData();
    obs.onNext(GetClusterCredentialsResponse.newBuilder()
        .setUsername(decode(data.get(StackGresPasswordKeys.SUPERUSER_USERNAME_KEY)))
        .setPassword(decode(data.get(StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY)))
        .build());
    obs.onCompleted();
  }

  // ---- helpers ----

  private Cluster byName(String name) {
    for (Cluster c : matriarch.listClusters()) {
      if (c.spec().name().equals(name)) {
        return c;
      }
    }
    return null;
  }

  private String namespaceFor(CreateClusterRequest req) {
    String ns = req.getPlacementMap().get("namespace");
    return ns == null || ns.isBlank() ? defaultNamespace : ns;
  }

  private String environmentId() {
    String id = installationInfoHolder.getInstallationId();
    return id == null || id.isBlank() ? "local" : id;
  }

  private ClusterOperationProgress frame(OperationStatus status, Cluster c, String envId) {
    return ClusterOperationProgress.newBuilder()
        .setStatus(status)
        .setCluster(ClusterProtoMapper.toProto(c, envId))
        .build();
  }

  private ClusterOperationProgress failedFrame(Cluster c, int code, String message, String envId) {
    ClusterOperationProgress.Builder b = ClusterOperationProgress.newBuilder()
        .setStatus(OperationStatus.OPERATION_STATUS_FAILED)
        .setError(com.google.rpc.Status.newBuilder().setCode(code).setMessage(message).build());
    if (c != null) {
      b.setCluster(ClusterProtoMapper.toProto(c, envId));
    }
    return b.build();
  }

  private static String decode(String base64) {
    return base64 == null ? "" : new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
  }

  private static StatusRuntimeException toGrpcError(KubernetesClientException e) {
    Status status = switch (e.getCode()) {
      case 409 -> Status.ALREADY_EXISTS;
      case 400, 422 -> Status.INVALID_ARGUMENT;
      case 401, 403 -> Status.PERMISSION_DENIED;
      case 404 -> Status.NOT_FOUND;
      default -> Status.INTERNAL;
    };
    return status.withDescription(e.getMessage()).asRuntimeException();
  }
}
