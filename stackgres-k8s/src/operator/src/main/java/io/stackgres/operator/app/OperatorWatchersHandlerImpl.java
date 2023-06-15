/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorageList;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfiguration;
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
        onCreateOrUpdate(
            reconcileCluster())));

    monitors.add(createWatcher(
        StackGresProfile.class,
        StackGresProfileList.class,
        onCreateOrUpdate(
            reconcileInstanceProfileClusters()
            .andThen(reconcileInstanceProfileDistributedLogs())
            .andThen(reconcileInstanceProfileShardedClusters()))));

    monitors.add(createWatcher(
        StackGresPostgresConfig.class,
        StackGresPostgresConfigList.class,
        onCreateOrUpdate(
            reconcilePostgresConfigClusters()
            .andThen(reconcilePostgresConfigDistributedLogs())
            .andThen(reconcilePostgresConfigShardedClusters()))));

    monitors.add(createWatcher(
        StackGresPoolingConfig.class,
        StackGresPoolingConfigList.class,
        onCreateOrUpdate(reconcilePoolingConfigClusters()
            .andThen(reconcilePoolingConfigShardedClusters()))));

    monitors.add(createWatcher(
        StackGresObjectStorage.class,
        StackGresObjectStorageList.class,
        onCreateOrUpdate(
            reconcileObjectStorageClusters()
            .andThen(reconcileObjectStorageShardedClusters()))));

    monitors.add(createWatcher(
        StackGresBackup.class,
        StackGresBackupList.class,
        onCreateOrUpdate(
            reconcileBackup())));

    monitors.add(createWatcher(
        StackGresDbOps.class,
        StackGresDbOpsList.class,
        onCreateOrUpdate(
            reconcileDbOps())));

    monitors.add(createWatcher(
        StackGresDistributedLogs.class,
        StackGresDistributedLogsList.class,
        onCreateOrUpdate(
            reconcileDistributedLogs())));

    monitors.add(createWatcher(
        StackGresShardedCluster.class,
        StackGresShardedClusterList.class,
        onCreateOrUpdate(
            reconcileShardedCluster())));
  }

  private <T extends CustomResource<?, ?>,
      L extends DefaultKubernetesResourceList<T>> WatcherMonitor<T> createWatcher(
      @NotNull Class<T> crClass, @NotNull Class<L> listClass,
      @NotNull BiConsumer<Action, T> consumer) {

    return new WatcherMonitor<>(crClass.getSimpleName(),
        watcherListener -> client
        .resources(crClass, listClass)
        .inAnyNamespace()
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
    return (action, cluster) -> clusterReconciliatorCycle.reconcile(cluster);
  }

  private BiConsumer<Action, StackGresDistributedLogs> reconcileDistributedLogs() {
    return (action, distributedlogs) -> distributedLogsReconciliatorCycle
        .reconcile(distributedlogs);
  }

  private BiConsumer<Action, StackGresShardedCluster> reconcileShardedCluster() {
    return (action, shardedCluster) -> shardedClusterReconciliatorCycle.reconcile(shardedCluster);
  }

  private BiConsumer<Action, StackGresDbOps> reconcileDbOps() {
    return (action, dbops) -> dbOpsReconciliatorCycle.reconcile(dbops);
  }

  private BiConsumer<Action, StackGresBackup> reconcileBackup() {
    return (action, backup) -> backupReconciliatorCycle.reconcile(backup);
  }

  private BiConsumer<Action, StackGresProfile> reconcileInstanceProfileClusters() {
    return (action, instanceProfile) -> client
        .resources(StackGresCluster.class, StackGresClusterList.class)
        .inNamespace(instanceProfile.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getSpec().getResourceProfile(),
            instanceProfile.getMetadata().getName()))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, StackGresPostgresConfig> reconcilePostgresConfigClusters() {
    return (action, postgresConfig) -> client
        .resources(StackGresCluster.class, StackGresClusterList.class)
        .inNamespace(postgresConfig.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getSpec().getConfiguration().getPostgresConfig(),
            postgresConfig.getMetadata().getName()))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, StackGresPoolingConfig> reconcilePoolingConfigClusters() {
    return (action, poolingConfig) -> client
        .resources(StackGresCluster.class, StackGresClusterList.class)
        .inNamespace(poolingConfig.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getSpec().getConfiguration().getConnectionPoolingConfig(),
            poolingConfig.getMetadata().getName()))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, StackGresObjectStorage> reconcileObjectStorageClusters() {
    return (action, objectStorage) -> client
        .resources(StackGresCluster.class, StackGresClusterList.class)
        .inNamespace(objectStorage.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(cluster -> Objects.equals(
            Optional.ofNullable(cluster.getSpec().getConfiguration().getBackups()).orElse(List.of())
            .stream().findFirst().map(StackGresClusterBackupConfiguration::getObjectStorage)
            .orElse(null),
            objectStorage.getMetadata().getName()))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, StackGresProfile> reconcileInstanceProfileDistributedLogs() {
    return (action, instanceProfile) -> client
        .resources(StackGresDistributedLogs.class, StackGresDistributedLogsList.class)
        .inNamespace(instanceProfile.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(distributedLogs -> Objects.equals(
            distributedLogs.getSpec().getResourceProfile(),
            instanceProfile.getMetadata().getName()))
        .forEach(distributedLogs -> reconcileDistributedLogs().accept(action, distributedLogs));
  }

  private BiConsumer<Action, StackGresPostgresConfig> reconcilePostgresConfigDistributedLogs() {
    return (action, postgresConfig) -> client
        .resources(StackGresDistributedLogs.class, StackGresDistributedLogsList.class)
        .inNamespace(postgresConfig.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(distributedLogs -> Objects.equals(
            distributedLogs.getSpec().getConfiguration().getPostgresConfig(),
            postgresConfig.getMetadata().getName()))
        .forEach(distributedLogs -> reconcileDistributedLogs().accept(action, distributedLogs));
  }

  private BiConsumer<Action, StackGresProfile> reconcileInstanceProfileShardedClusters() {
    return (action, instanceProfile) -> client
        .resources(StackGresShardedCluster.class, StackGresShardedClusterList.class)
        .inNamespace(instanceProfile.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(shardedCluster -> Objects.equals(
            shardedCluster.getSpec().getCoordinator().getResourceProfile(),
            instanceProfile.getMetadata().getName())
            || Objects.equals(
                shardedCluster.getSpec().getShards().getResourceProfile(),
                instanceProfile.getMetadata().getName())
            || Optional.ofNullable(shardedCluster.getSpec()
                .getShards().getOverrides()).orElse(List.of())
            .stream().anyMatch(spec -> Objects
                .equals(
                    spec.getResourceProfile(),
                    instanceProfile.getMetadata().getName())))
        .forEach(shardedCluster -> reconcileShardedCluster().accept(action, shardedCluster));
  }

  private BiConsumer<Action, StackGresPostgresConfig> reconcilePostgresConfigShardedClusters() {
    return (action, postgresConfig) -> client
        .resources(StackGresShardedCluster.class, StackGresShardedClusterList.class)
        .inNamespace(postgresConfig.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(shardedCluster -> Objects.equals(
            shardedCluster.getSpec().getCoordinator().getConfiguration().getPostgresConfig(),
            postgresConfig.getMetadata().getName())
            || Objects.equals(
                shardedCluster.getSpec().getShards().getConfiguration().getPostgresConfig(),
                postgresConfig.getMetadata().getName())
            || Optional.ofNullable(shardedCluster.getSpec()
                .getShards().getOverrides()).orElse(List.of())
            .stream().anyMatch(spec -> spec.getConfiguration() != null
                && Objects.equals(
                    spec.getConfiguration().getPostgresConfig(),
                    postgresConfig.getMetadata().getName())))
        .forEach(shardedCluster -> reconcileShardedCluster().accept(action, shardedCluster));
  }

  private BiConsumer<Action, StackGresPoolingConfig> reconcilePoolingConfigShardedClusters() {
    return (action, poolingConfig) -> client
        .resources(StackGresShardedCluster.class, StackGresShardedClusterList.class)
        .inNamespace(poolingConfig.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(shardedCluster -> Objects.equals(
            shardedCluster.getSpec()
            .getCoordinator().getConfiguration().getConnectionPoolingConfig(),
            poolingConfig.getMetadata().getName())
            || Objects.equals(
                shardedCluster.getSpec()
                .getShards().getConfiguration().getConnectionPoolingConfig(),
                poolingConfig.getMetadata().getName())
            || Optional.ofNullable(shardedCluster.getSpec().getShards().getOverrides())
            .orElse(List.of())
            .stream().anyMatch(spec -> spec.getConfiguration() != null
                && Objects.equals(
                    spec.getConfiguration().getConnectionPoolingConfig(),
                    poolingConfig.getMetadata().getName())))
        .forEach(shardedCluster -> reconcileShardedCluster().accept(action, shardedCluster));
  }

  private BiConsumer<Action, StackGresObjectStorage> reconcileObjectStorageShardedClusters() {
    return (action, objectStorage) -> client
        .resources(StackGresShardedCluster.class, StackGresShardedClusterList.class)
        .inNamespace(objectStorage.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(shardedCluster -> Objects.equals(
            Optional.ofNullable(shardedCluster.getSpec().getConfiguration().getBackups())
            .orElse(List.of())
            .stream().findFirst().map(StackGresShardedClusterBackupConfiguration::getObjectStorage)
            .orElse(null),
            objectStorage.getMetadata().getName()))
        .forEach(shardedCluster -> reconcileShardedCluster().accept(action, shardedCluster));
  }

  @Override
  public void stopWatchers() {
    monitors.forEach(WatcherMonitor::close);
  }

}
