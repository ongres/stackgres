/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.distributedlogs.controller.DistributedLogsControllerReconciliationCycle;
import io.stackgres.distributedlogs.controller.ResourceWatcherFactory;
import io.stackgres.operatorframework.resource.WatcherMonitor;

@ApplicationScoped
public class DistributedLogsControllerWatchersHandlerImpl
    implements DistributedLogsControllerWatcherHandler {

  private final List<WatcherMonitor<?>> monitors = new ArrayList<>();

  private final KubernetesClient client;
  private final DistributedLogsControllerReconciliationCycle distributedLogsReconciliationCycle;
  private final ResourceWatcherFactory watcherFactory;

  @Inject
  public DistributedLogsControllerWatchersHandlerImpl(KubernetesClient client,
      DistributedLogsControllerReconciliationCycle distributedLogsReconciliationCycle,
      ResourceWatcherFactory watcherFactory) {
    this.client = client;
    this.distributedLogsReconciliationCycle = distributedLogsReconciliationCycle;
    this.watcherFactory = watcherFactory;
  }

  @Override
  public void startWatchers() {
    monitors.add(createWatcher(
        StackGresDistributedLogs.class,
        StackGresDistributedLogsList.class,
        onCreateOrUpdate(
            reconcileDistributedLogs())));
  }

  private <T extends CustomResource<?, ?>,
      L extends KubernetesResourceList<T>> WatcherMonitor<T> createWatcher(
      Class<T> crClass, Class<L> listClass, BiConsumer<Action, T> consumer) {
    return new WatcherMonitor<>(crClass.getSimpleName(),
        watcherListener -> client
        .resources(crClass, listClass)
        .inNamespace(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAMESPACE.getString())
        .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAME.getString())
        .watch(watcherFactory.createWatcher(consumer, watcherListener)));
  }

  private <T> BiConsumer<Action, T> onCreateOrUpdate(BiConsumer<Action, T> consumer) {
    return (action, resource) -> {
      if (action != Action.DELETED) {
        consumer.accept(action, resource);
      }
    };
  }

  private BiConsumer<Action, StackGresDistributedLogs> reconcileDistributedLogs() {
    return (action, distributedLogs) -> distributedLogsReconciliationCycle
        .reconcile(distributedLogs);
  }

  @Override
  public void stopWatchers() {
    monitors.forEach(WatcherMonitor::close);
  }

}
