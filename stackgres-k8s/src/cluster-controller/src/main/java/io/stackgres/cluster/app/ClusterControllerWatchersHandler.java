/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.cluster.controller.ClusterControllerReconciliationCycle;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.operatorframework.resource.WatcherMonitor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterControllerWatchersHandler {

  private final List<WatcherMonitor<?>> monitors = new ArrayList<>();

  private final KubernetesClient client;
  private final ClusterControllerReconciliationCycle clusterReconciliationCycle;
  private final AtomicReference<Optional<StackGresCluster>> clusterReference =
      new AtomicReference<>(Optional.empty());
  private final AtomicBoolean wasLeaderReference = new AtomicBoolean(false);
  private final String podName;

  @Inject
  public ClusterControllerWatchersHandler(
      ClusterControllerPropertyContext propertyContext,
      KubernetesClient client,
      ClusterControllerReconciliationCycle clusterReconciliationCycle) {
    this.client = client;
    this.clusterReconciliationCycle = clusterReconciliationCycle;
    this.podName = propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
  }

  public void startWatchers() {
    monitors.add(createClusterWatcher(
        StackGresCluster.class,
        StackGresClusterList.class,
        onCreateOrUpdate(
            reconcileCluster())));

    monitors.add(createEndpointsWatcher(
        Endpoints.class,
        EndpointsList.class,
        onCreateOrUpdate(
            recponcileClusterEndpoints())));
  }

  private <T extends HasMetadata,
      L extends KubernetesResourceList<T>> WatcherMonitor<T> createClusterWatcher(
      Class<T> crClass, Class<L> listClass, BiConsumer<Action, T> consumer) {
    return new WatcherMonitor<>(
        crClass.getSimpleName(),
        watcher -> client
        .resources(crClass, listClass)
        .inNamespace(ClusterControllerProperty.CLUSTER_NAMESPACE.getString())
        .withName(ClusterControllerProperty.CLUSTER_NAME.getString())
        .watch(watcher),
        consumer);
  }

  private <T extends HasMetadata,
      L extends KubernetesResourceList<T>> WatcherMonitor<T> createEndpointsWatcher(
      Class<T> crClass, Class<L> listClass, BiConsumer<Action, T> consumer) {
    return new WatcherMonitor<>(
        crClass.getSimpleName(),
        watcher -> client
        .resources(crClass, listClass)
        .inNamespace(ClusterControllerProperty.CLUSTER_NAMESPACE.getString())
        .withName(ClusterControllerProperty.CLUSTER_ENDPOINTS_NAME.getString())
        .watch(watcher),
        consumer);
  }

  private <T> BiConsumer<Action, T> onCreateOrUpdate(BiConsumer<Action, T> consumer) {
    return (action, resource) -> {
      if (action != Action.DELETED) {
        consumer.accept(action, resource);
      }
    };
  }

  private BiConsumer<Action, StackGresCluster> reconcileCluster() {
    return (action, cluster) -> reconcileCluster(cluster);
  }

  private void reconcileCluster(StackGresCluster cluster) {
    clusterReference.set(Optional.of(cluster));
    clusterReconciliationCycle.reconcile(cluster);
  }

  private BiConsumer<Action, Endpoints> recponcileClusterEndpoints() {
    return (action, endpoints) -> recponcileClusterEndpoints(endpoints);
  }

  private void recponcileClusterEndpoints(Endpoints endpoints) {
    if (Objects.equals(
        podName,
        Optional.of(endpoints.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(PatroniUtil.LEADER_KEY))
        .orElse(null))) {
      if (!wasLeaderReference.get()) {
        clusterReference.get()
            .ifPresent(clusterReconciliationCycle::reconcile);
        wasLeaderReference.set(true);
      }
    } else {
      if (wasLeaderReference.get()) {
        clusterReference.get()
            .ifPresent(clusterReconciliationCycle::reconcile);
        wasLeaderReference.set(false);
      }
    }
  }

  public void stopWatchers() {
    monitors.forEach(WatcherMonitor::close);
  }

}
