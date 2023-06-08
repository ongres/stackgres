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

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterList;
import io.stackgres.operator.conciliation.backup.BackupReconciliator;
import io.stackgres.operator.conciliation.cluster.ClusterReconciliator;
import io.stackgres.operator.conciliation.dbops.DbOpsReconciliator;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsReconciliator;
import io.stackgres.operator.conciliation.shardedcluster.ShardedClusterReconciliator;
import io.stackgres.operator.controller.ResourceWatcherFactory;
import io.stackgres.operatorframework.resource.WatcherMonitor;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class OperatorWatchersHandlerImpl implements OperatorWatcherHandler {

  private final List<WatcherMonitor<?>> monitors = new ArrayList<>();

  private final KubernetesClient client;
  private final ClusterReconciliator clusterReconciliatorCycle;
  private final DistributedLogsReconciliator distributedLogsReconciliatorCycle;
  private final DbOpsReconciliator dbOpsReconciliatorCycle;
  private final BackupReconciliator backupReconciliatorCycle;
  private final ShardedClusterReconciliator shardedClusterReconciliatorCycle;
  private final ResourceWatcherFactory watcherFactory;

  @Inject
  public OperatorWatchersHandlerImpl(
      KubernetesClient client,
      ClusterReconciliator clusterReconciliatorCycle,
      DistributedLogsReconciliator distributedLogsReconciliatorCycle,
      DbOpsReconciliator dbOpsReconciliatorCycle,
      BackupReconciliator backupReconciliatorCycle,
      ShardedClusterReconciliator shardedClusterReconciliatorCycle,
      ResourceWatcherFactory watcherFactory) {
    super();
    this.client = client;
    this.clusterReconciliatorCycle = clusterReconciliatorCycle;
    this.distributedLogsReconciliatorCycle = distributedLogsReconciliatorCycle;
    this.dbOpsReconciliatorCycle = dbOpsReconciliatorCycle;
    this.backupReconciliatorCycle = backupReconciliatorCycle;
    this.shardedClusterReconciliatorCycle = shardedClusterReconciliatorCycle;
    this.watcherFactory = watcherFactory;
  }

  @Override
  public void startWatchers() {
    monitors.add(createWatcher(
        StackGresCluster.class,
        StackGresClusterList.class,
        reconcileCluster()
            .andThen(reconcileDistributedLogs())));

    monitors.add(createWatcher(
        StackGresPostgresConfig.class,
        StackGresPostgresConfigList.class,
        reconcileCluster()));

    monitors.add(createWatcher(
        StackGresPoolingConfig.class,
        StackGresPoolingConfigList.class,
        reconcileCluster()));

    monitors.add(createWatcher(
        StackGresProfile.class,
        StackGresProfileList.class,
        reconcileCluster()));

    monitors.add(createWatcher(
        StackGresBackupConfig.class,
        StackGresBackupConfigList.class,
        reconcileCluster()));

    monitors.add(createWatcher(
        StackGresBackup.class,
        StackGresBackupList.class,
        reconcileBackup()));

    monitors.add(createWatcher(
        StackGresDbOps.class,
        StackGresDbOpsList.class,
        reconcileDbOps()));

    monitors.add(createWatcher(
        StackGresDistributedLogs.class,
        StackGresDistributedLogsList.class,
        reconcileDistributedLogs()));

    monitors.add(createWatcher(
        StackGresShardedCluster.class,
        StackGresShardedClusterList.class,
        reconcileShardedCluster()));
  }

  private <T extends CustomResource<?, ?>,
      L extends DefaultKubernetesResourceList<T>> WatcherMonitor<T> createWatcher(
      @NotNull Class<T> crClass, @NotNull Class<L> listClass, @NotNull Consumer<Action> consumer) {

    return new WatcherMonitor<>(crClass.getSimpleName(),
        watcherListener -> client
        .resources(crClass, listClass)
        .inAnyNamespace()
        .watch(watcherFactory.createWatcher(consumer, watcherListener)));
  }

  private Consumer<Action> reconcileCluster() {
    return action -> clusterReconciliatorCycle.reconcile();
  }

  private Consumer<Action> reconcileDistributedLogs() {
    return action -> distributedLogsReconciliatorCycle.reconcile();
  }

  private Consumer<Action> reconcileDbOps() {
    return action -> dbOpsReconciliatorCycle.reconcile();
  }

  private Consumer<Action> reconcileBackup() {
    return action -> backupReconciliatorCycle.reconcile();
  }

  private Consumer<Action> reconcileShardedCluster() {
    return action -> shardedClusterReconciliatorCycle.reconcile();
  }

  @Override
  public void stopWatchers() {
    monitors.forEach(WatcherMonitor::close);
  }

}
