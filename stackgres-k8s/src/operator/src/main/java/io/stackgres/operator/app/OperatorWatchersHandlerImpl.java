/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.quarkus.runtime.Application;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackupDoneable;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigDoneable;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDefinition;
import io.stackgres.common.crd.sgcluster.StackGresClusterDoneable;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDefinition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDoneable;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigDefinition;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigDoneable;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileDefinition;
import io.stackgres.common.crd.sgprofile.StackGresProfileDoneable;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import io.stackgres.operator.controller.ClusterReconciliationCycle;
import io.stackgres.operator.controller.ClusterResourceWatcherFactory;
import io.stackgres.operator.controller.DistributedLogsReconciliationCycle;
import io.stackgres.operator.controller.WatcherMonitor;
import io.stackgres.operatorframework.resource.ResourceUtil;

@ApplicationScoped
public class OperatorWatchersHandlerImpl implements OperatorWatcherHandler {

  private final List<WatcherMonitor<?>> monitors = new ArrayList<>();

  private final KubernetesClientFactory kubeClient;
  private final ClusterReconciliationCycle clusterReconciliationCycle;
  private final DistributedLogsReconciliationCycle distributedLogsReconciliationCycle;
  private final ClusterResourceWatcherFactory watcherFactory;

  @Inject
  public OperatorWatchersHandlerImpl(KubernetesClientFactory kubeClient,
      ClusterReconciliationCycle clusterReconciliationCycle,
      DistributedLogsReconciliationCycle distributedLogsReconciliationCycle,
      ClusterResourceWatcherFactory watcherFactory) {
    this.kubeClient = kubeClient;
    this.clusterReconciliationCycle = clusterReconciliationCycle;
    this.distributedLogsReconciliationCycle = distributedLogsReconciliationCycle;
    this.watcherFactory = watcherFactory;
  }

  @Override
  public void startWatchers() {
    monitors.add(createWatcher(
        StackGresClusterDefinition.NAME,
        StackGresCluster.class,
        StackGresClusterList.class,
        StackGresClusterDoneable.class,
        reconcileCluster()
          .andThen(reconcileDistributedLogs())));

    monitors.add(createWatcher(
        StackGresPostgresConfigDefinition.NAME,
        StackGresPostgresConfig.class,
        StackGresPostgresConfigList.class,
        StackGresPostgresConfigDoneable.class,
        reconcileCluster()));

    monitors.add(createWatcher(
        StackGresPoolingConfigDefinition.NAME,
        StackGresPoolingConfig.class,
        StackGresPoolingConfigList.class,
        StackGresPoolingConfigDoneable.class,
        reconcileCluster()));

    monitors.add(createWatcher(
        StackGresProfileDefinition.NAME,
        StackGresProfile.class,
        StackGresProfileList.class,
        StackGresProfileDoneable.class,
        reconcileCluster()));

    monitors.add(createWatcher(
        StackGresBackupConfigDefinition.NAME,
        StackGresBackupConfig.class,
        StackGresBackupConfigList.class,
        StackGresBackupConfigDoneable.class,
        reconcileCluster()));

    monitors.add(createWatcher(
        StackGresBackupDefinition.NAME,
        StackGresBackup.class,
        StackGresBackupList.class,
        StackGresBackupDoneable.class,
        reconcileCluster()));

    monitors.add(createWatcher(
        StackGresDistributedLogsDefinition.NAME,
        StackGresDistributedLogs.class,
        StackGresDistributedLogsList.class,
        StackGresDistributedLogsDoneable.class,
        reconcileDistributedLogs()));

  }

  private <R extends HasMetadata, L extends KubernetesResourceList<R>, D extends Doneable<R>>
      WatcherMonitor<R> createWatcher(String name, Class<R> crClass, Class<L> listClass,
          Class<D> doneableClass, Consumer<Action> consumer) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(client, name)
          .orElseThrow(() -> new IllegalStateException("Some required CRDs does not exists"));
      return new WatcherMonitor<>(watcherListener -> kubeClient.create()
          .customResources(crd, crClass, listClass, doneableClass)
          .inAnyNamespace()
          .watch(watcherFactory.createWatcher(consumer, watcherListener)),
          () -> new Thread(() -> Application.currentApplication().stop()).start());
    }
  }

  private Consumer<Action> reconcileCluster() {
    return action -> clusterReconciliationCycle.reconcile();
  }

  private Consumer<Action> reconcileDistributedLogs() {
    return action -> distributedLogsReconciliationCycle.reconcile();
  }

  @Override
  public void stopWatchers() {
    monitors.forEach(WatcherMonitor::close);
  }

}
