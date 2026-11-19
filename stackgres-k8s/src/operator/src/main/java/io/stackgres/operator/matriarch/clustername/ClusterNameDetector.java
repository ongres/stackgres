/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch.clustername;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

/**
 * Phase 1 (detect-and-log only): resolves a best-effort name for the Kubernetes cluster the operator
 * runs in and logs it. Nothing is persisted, added to the {@code Environment} proto, sent over the
 * cloud uplink, or shown in the UI — this exists so the detection chain can be validated from the
 * operator logs, on any cloud or local cluster, before any wire/identity work (see
 * {@code matriarch/doc/environment-cluster-name-plan.md}).
 *
 * <p>Resolution order, first non-empty wins:
 * <ol>
 *   <li>explicit override {@code STACKGRES_CLUSTER_NAME};</li>
 *   <li>cloud metadata (GKE reliable; EKS/AKS best-effort), probed in parallel with short timeouts;</li>
 *   <li>{@code kind}: a Node {@code spec.providerID} of the form {@code kind://<driver>/<cluster>/<node>};</li>
 *   <li>control-plane Node name (minikube profile and similar), best-effort;</li>
 *   <li>fallback: the {@code kube-system} namespace UID — a guaranteed, unique, stable floor.</li>
 * </ol>
 *
 * <p>Gated by {@code stackgres.cloud.enabled} (the same flag as the uplink) so stock installs get no
 * new behaviour or node reads. Every step is isolated: a failure or missing permission is logged and
 * falls through; detection never fails operator startup.
 */
@ApplicationScoped
public class ClusterNameDetector {

  private static final Logger LOG = Logger.getLogger(ClusterNameDetector.class);

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(1);
  private static final int OVERALL_TIMEOUT_SECONDS = 3;
  // Priority order: prefer the friendly cloud name over local-cluster heuristics.
  private static final List<CloudClusterNameProbe> CLOUD_PROBES = List.of(
      new GcpClusterNameProbe(),
      new AwsClusterNameProbe(),
      new AzureClusterNameProbe());

  @Inject
  KubernetesClient client;

  @Inject
  Config config;

  /** A resolved name together with which step produced it. */
  public record Detected(String name, String source) {
  }

  void onStart(@Observes StartupEvent ev) {
    try {
      Optional<Detected> detected = detect();
      if (detected.isPresent()) {
        LOG.infof("detected cluster name: %s (source=%s)",
            detected.get().name(), detected.get().source());
      } else {
        LOG.info("no cluster name detected");
      }
    } catch (RuntimeException e) {
      LOG.warnf("cluster name detection failed: %s", e.getMessage());
    }
  }

  /** Run the resolution chain. Public/package-visible for wiring and tests. */
  public Optional<Detected> detect() {
    Optional<String> override = config.getOptionalValue("STACKGRES_CLUSTER_NAME", String.class)
        .map(String::strip)
        .filter(s -> !s.isBlank());
    if (override.isPresent()) {
      return Optional.of(new Detected(override.get(), "override"));
    }

    Optional<Detected> cloud = detectCloud();
    if (cloud.isPresent()) {
      return cloud;
    }

    Optional<String> kind = detectKind();
    if (kind.isPresent()) {
      return Optional.of(new Detected(kind.get(), "kind"));
    }

    Optional<String> nodeName = detectNodeName();
    if (nodeName.isPresent()) {
      return Optional.of(new Detected(nodeName.get(), "node-name"));
    }

    Optional<String> uid = detectNamespaceUid();
    if (uid.isPresent()) {
      return Optional.of(new Detected(uid.get(), "ns-uid"));
    }

    return Optional.empty();
  }

  private Optional<Detected> detectCloud() {
    try (HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build()) {
      List<CompletableFuture<Optional<String>>> futures = CLOUD_PROBES.stream()
          .map(probe -> CompletableFuture.supplyAsync(() -> probe.detect(httpClient)))
          .toList();
      try {
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
            .get(OVERALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException | TimeoutException e) {
        // some or all probes timed out or failed — check whatever completed below
        LOG.debugf("cloud probes did not all complete: %s", e.getMessage());
      }
      for (int i = 0; i < CLOUD_PROBES.size(); i++) {
        Optional<String> result = getNow(futures.get(i));
        if (result.isPresent()) {
          return Optional.of(new Detected(result.get(), CLOUD_PROBES.get(i).source()));
        }
      }
    }
    return Optional.empty();
  }

  private Optional<String> detectKind() {
    try {
      return client.nodes().list().getItems().stream()
          .map(node -> node.getSpec() == null ? null : node.getSpec().getProviderID())
          .map(ClusterNameDetector::parseKindProviderId)
          .flatMap(Optional::stream)
          .findFirst();
    } catch (RuntimeException e) {
      LOG.debugf("kind detection failed: %s", e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Parse {@code kind://<driver>/<cluster>/<node>} (kind sets this as the node providerID). Only the
   * {@code kind://} scheme carries a cluster name — real clouds use {@code aws:///}, {@code gce://},
   * {@code azure://} which encode the instance, not the cluster. Package-visible for unit testing.
   */
  static Optional<String> parseKindProviderId(String providerId) {
    if (providerId == null || !providerId.startsWith("kind://")) {
      return Optional.empty();
    }
    String[] parts = providerId.substring("kind://".length()).split("/");
    // parts = [driver, cluster, node]
    if (parts.length >= 2 && !parts[1].isBlank()) {
      return Optional.of(parts[1]);
    }
    return Optional.empty();
  }

  private Optional<String> detectNodeName() {
    try {
      return client.nodes().list().getItems().stream()
          .filter(ClusterNameDetector::isControlPlane)
          .map(node -> node.getMetadata().getName())
          .filter(name -> name != null && !name.isBlank())
          .map(ClusterNameDetector::stripControlPlaneSuffix)
          .findFirst();
    } catch (RuntimeException e) {
      LOG.debugf("node-name detection failed: %s", e.getMessage());
      return Optional.empty();
    }
  }

  private static boolean isControlPlane(Node node) {
    if (node.getMetadata() == null || node.getMetadata().getLabels() == null) {
      return false;
    }
    return node.getMetadata().getLabels().containsKey("node-role.kubernetes.io/control-plane")
        || node.getMetadata().getLabels().containsKey("node-role.kubernetes.io/master");
  }

  /**
   * kind names control-plane nodes {@code <cluster>-control-plane}; minikube's node is the profile.
   */
  static String stripControlPlaneSuffix(String nodeName) {
    if (nodeName.endsWith("-control-plane")) {
      return nodeName.substring(0, nodeName.length() - "-control-plane".length());
    }
    if (nodeName.endsWith("-master")) {
      return nodeName.substring(0, nodeName.length() - "-master".length());
    }
    return nodeName;
  }

  private Optional<String> detectNamespaceUid() {
    try {
      var namespace = client.namespaces().withName("kube-system").get();
      if (namespace != null && namespace.getMetadata() != null) {
        String uid = namespace.getMetadata().getUid();
        if (uid != null && !uid.isBlank()) {
          return Optional.of(uid);
        }
      }
    } catch (RuntimeException e) {
      LOG.debugf("ns-uid detection failed: %s", e.getMessage());
    }
    return Optional.empty();
  }

  private static Optional<String> getNow(CompletableFuture<Optional<String>> future) {
    try {
      return future.getNow(Optional.empty());
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }

}
