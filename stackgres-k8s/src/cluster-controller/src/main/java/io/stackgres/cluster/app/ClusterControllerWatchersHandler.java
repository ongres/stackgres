/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.stackgres.cluster.controller.ClusterControllerReconciliationCycle;
import io.stackgres.cluster.controller.ResourceWatcherFactory;
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
  private final ResourceWatcherFactory watcherFactory;

  @Inject
  public ClusterControllerWatchersHandler(KubernetesClient client,
      ClusterControllerReconciliationCycle clusterReconciliationCycle,
      ResourceWatcherFactory watcherFactory) {
    this.client = client;
    this.clusterReconciliationCycle = clusterReconciliationCycle;
    this.watcherFactory = watcherFactory;
  }

  public void startWatchers() {
    monitors.add(createWatcher(
        StackGresCluster.class,
        StackGresClusterList.class,
        onCreateOrUpdate(
            reconcileCluster())));

    monitors.add(createClusterSecretWatcher(
        onCreateOrUpdate(
            reconcileClusterSecret())));
  }

  private <T extends CustomResource<?, ?>,
      L extends KubernetesResourceList<T>> WatcherMonitor<T> createWatcher(
      Class<T> crClass, Class<L> listClass, BiConsumer<Action, T> consumer) {
    return new WatcherMonitor<>(crClass.getSimpleName(),
        watcherListener -> client
        .resources(crClass, listClass)
        .inNamespace(ClusterControllerProperty.CLUSTER_NAMESPACE.getString())
        .withName(ClusterControllerProperty.CLUSTER_NAME.getString())
        .watch(watcherFactory.createWatcher(consumer, watcherListener)));
  }

  /**
   * The Secret of the cluster holds the credentials, that are applied to Patroni and to Postgres
   * as soon as they change instead of waiting for the next reconciliation period.
   */
  private WatcherMonitor<Secret> createClusterSecretWatcher(
      BiConsumer<Action, Secret> consumer) {
    return new WatcherMonitor<>(Secret.class.getSimpleName(),
        watcherListener -> client
        .secrets()
        .inNamespace(ClusterControllerProperty.CLUSTER_NAMESPACE.getString())
        .withName(PatroniUtil.secretName(ClusterControllerProperty.CLUSTER_NAME.getString()))
        .watch(watcherFactory.createWatcher(consumer, watcherListener)));
  }

  private <T> BiConsumer<Action, T> onCreateOrUpdate(BiConsumer<Action, T> consumer) {
    return (action, resource) -> {
      if (action != Action.DELETED) {
        consumer.accept(action, resource);
      }
    };
  }

  private BiConsumer<Action, StackGresCluster> reconcileCluster() {
    return (action, cluster) -> clusterReconciliationCycle.reconcile(cluster);
  }

  private BiConsumer<Action, Secret> reconcileClusterSecret() {
    return (action, secret) -> clusterReconciliationCycle.reconcileAll();
  }

  public void stopWatchers() {
    monitors.forEach(WatcherMonitor::close);
  }

}
