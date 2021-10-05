/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.stackgres.cluster.controller.ClusterControllerReconciliationCycle;
import io.stackgres.cluster.controller.ResourceWatcherFactory;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.operatorframework.resource.WatcherMonitor;

@ApplicationScoped
public class ClusterControllerWatchersHandlerImpl
    implements ClusterControllerWatcherHandler {

  private final List<WatcherMonitor<?>> monitors = new ArrayList<>();

  private final KubernetesClient client;
  private final ClusterControllerReconciliationCycle clusterReconciliationCycle;
  private final ResourceWatcherFactory watcherFactory;

  @Inject
  public ClusterControllerWatchersHandlerImpl(KubernetesClient client,
      ClusterControllerReconciliationCycle clusterReconciliationCycle,
      ResourceWatcherFactory watcherFactory) {
    this.client = client;
    this.clusterReconciliationCycle = clusterReconciliationCycle;
    this.watcherFactory = watcherFactory;
  }

  @Override
  public void startWatchers() {
    monitors.add(createWatcher(
        StackGresCluster.class,
        StackGresClusterList.class,
        reconcileCluster()));

  }

  private <T extends CustomResource<?, ?>,
      L extends KubernetesResourceList<T>> WatcherMonitor<T> createWatcher(
      Class<T> crClass, Class<L> listClass, Consumer<Action> consumer) {
    return new WatcherMonitor<>(crClass.getSimpleName(),
        watcherListener -> client
        .resources(crClass, listClass)
        .inNamespace(ClusterControllerProperty.CLUSTER_NAMESPACE.getString())
        .watch(watcherFactory.createWatcher(consumer, watcherListener)));
  }

  private Consumer<Action> reconcileCluster() {
    return action -> clusterReconciliationCycle.reconcile();
  }

  @Override
  public void stopWatchers() {
    monitors.forEach(WatcherMonitor::close);
  }

}
