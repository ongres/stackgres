/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.quarkus.runtime.Application;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDefinition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDoneable;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.distributedlogs.common.DistributedLogsProperty;
import io.stackgres.distributedlogs.controller.DistributedLogsReconciliationCycle;
import io.stackgres.distributedlogs.controller.ResourceWatcherFactory;
import io.stackgres.operatorframework.resource.WatcherMonitor;

@ApplicationScoped
public class OperatorWatchersHandlerImpl implements OperatorWatcherHandler {

  private final List<WatcherMonitor<?>> monitors = new ArrayList<>();

  private final KubernetesClientFactory clientFactory;
  private final DistributedLogsReconciliationCycle distributedLogsReconciliationCycle;
  private final ResourceWatcherFactory watcherFactory;

  @Inject
  public OperatorWatchersHandlerImpl(KubernetesClientFactory clientFactory,
      DistributedLogsReconciliationCycle distributedLogsReconciliationCycle,
      ResourceWatcherFactory watcherFactory) {
    this.clientFactory = clientFactory;
    this.distributedLogsReconciliationCycle = distributedLogsReconciliationCycle;
    this.watcherFactory = watcherFactory;
  }

  @Override
  public void startWatchers() {
    monitors.add(createWatcher(
        StackGresDistributedLogsDefinition.CONTEXT,
        StackGresDistributedLogs.class,
        StackGresDistributedLogsList.class,
        StackGresDistributedLogsDoneable.class,
        reconcileDistributedLogs()));

  }

  private <R extends HasMetadata, L extends KubernetesResourceList<R>, D extends Doneable<R>>
      WatcherMonitor<R> createWatcher(
          CustomResourceDefinitionContext customResourceDefinitionContext,
          Class<R> crClass, Class<L> listClass,
          Class<D> doneableClass, Consumer<Action> consumer) {

    try (KubernetesClient client = clientFactory.create()) {
      return new WatcherMonitor<>(watcherListener -> clientFactory.create()
          .customResources(customResourceDefinitionContext, crClass, listClass, doneableClass)
          .inNamespace(DistributedLogsProperty.DISTRIBUTEDLOGS_NAMESPACE.getString())
          .watch(watcherFactory.createWatcher(consumer, watcherListener)),
          () -> new Thread(() -> Application.currentApplication().stop()).start());
    }
  }

  private Consumer<Action> reconcileDistributedLogs() {
    return action -> distributedLogsReconciliationCycle.reconcile();
  }

  @Override
  public void stopWatchers() {
    monitors.forEach(WatcherMonitor::close);
  }

}
