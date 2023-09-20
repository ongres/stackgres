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
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigList;
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
import io.stackgres.operator.conciliation.config.ConfigReconciliator;
import io.stackgres.operator.conciliation.dbops.DbOpsReconciliator;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsReconciliator;
import io.stackgres.operator.conciliation.shardedcluster.ShardedClusterReconciliator;
import io.stackgres.operator.controller.ResourceWatcherFactory;
import io.stackgres.operatorframework.resource.WatcherMonitor;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class DefaultOperatorWatchersHandler implements OperatorWatchersHandler {

  private final List<WatcherMonitor<?>> monitors = new ArrayList<>();

  private final KubernetesClient client;
  private final ConfigReconciliator configReconciliatorCycle;
  private final ClusterReconciliator clusterReconciliatorCycle;
  private final DistributedLogsReconciliator distributedLogsReconciliatorCycle;
  private final DbOpsReconciliator dbOpsReconciliatorCycle;
  private final BackupReconciliator backupReconciliatorCycle;
  private final ShardedClusterReconciliator shardedClusterReconciliatorCycle;
  private final ResourceWatcherFactory watcherFactory;

  @Inject
  public DefaultOperatorWatchersHandler(
      KubernetesClient client,
      ConfigReconciliator configReconciliatorCycle,
      ClusterReconciliator clusterReconciliatorCycle,
      DistributedLogsReconciliator distributedLogsReconciliatorCycle,
      DbOpsReconciliator dbOpsReconciliatorCycle,
      BackupReconciliator backupReconciliatorCycle,
      ShardedClusterReconciliator shardedClusterReconciliatorCycle,
      ResourceWatcherFactory watcherFactory) {
    super();
    this.client = client;
    this.configReconciliatorCycle = configReconciliatorCycle;
    this.clusterReconciliatorCycle = clusterReconciliatorCycle;
    this.distributedLogsReconciliatorCycle = distributedLogsReconciliatorCycle;
    this.dbOpsReconciliatorCycle = dbOpsReconciliatorCycle;
    this.backupReconciliatorCycle = backupReconciliatorCycle;
    this.shardedClusterReconciliatorCycle = shardedClusterReconciliatorCycle;
    this.watcherFactory = watcherFactory;
  }

  @Override
  public void startWatchers() {
    monitors.add(createCustomResourceWatcher(
        StackGresConfig.class,
        StackGresConfigList.class,
        onCreateOrUpdate(
            reconcileConfig())));

    monitors.add(createCustomResourceWatcher(
        StackGresCluster.class,
        StackGresClusterList.class,
        onCreateOrUpdate(
            reconcileCluster())));

    monitors.add(createCustomResourceWatcher(
        StackGresProfile.class,
        StackGresProfileList.class,
        onCreateOrUpdate(
            reconcileInstanceProfileClusters()
            .andThen(reconcileInstanceProfileDistributedLogs())
            .andThen(reconcileInstanceProfileShardedClusters()))));

    monitors.add(createCustomResourceWatcher(
        StackGresPostgresConfig.class,
        StackGresPostgresConfigList.class,
        onCreateOrUpdate(
            reconcilePostgresConfigClusters()
            .andThen(reconcilePostgresConfigDistributedLogs())
            .andThen(reconcilePostgresConfigShardedClusters()))));

    monitors.add(createCustomResourceWatcher(
        StackGresPoolingConfig.class,
        StackGresPoolingConfigList.class,
        onCreateOrUpdate(reconcilePoolingConfigClusters()
            .andThen(reconcilePoolingConfigShardedClusters()))));

    monitors.add(createCustomResourceWatcher(
        StackGresObjectStorage.class,
        StackGresObjectStorageList.class,
        onCreateOrUpdate(
            reconcileObjectStorageClusters()
            .andThen(reconcileObjectStorageShardedClusters()))));

    monitors.add(createCustomResourceWatcher(
        StackGresBackup.class,
        StackGresBackupList.class,
        onCreateOrUpdate(
            reconcileBackup())));

    monitors.add(createCustomResourceWatcher(
        StackGresDbOps.class,
        StackGresDbOpsList.class,
        onCreateOrUpdate(
            reconcileDbOps())));

    monitors.add(createCustomResourceWatcher(
        StackGresDistributedLogs.class,
        StackGresDistributedLogsList.class,
        onCreateOrUpdate(
            reconcileDistributedLogs())));

    monitors.add(createCustomResourceWatcher(
        StackGresShardedCluster.class,
        StackGresShardedClusterList.class,
        onCreateOrUpdate(
            reconcileShardedCluster())));

    monitors.add(createWatcher(
        Endpoints.class,
        EndpointsList.class,
        onCreateOrUpdate(
            reconcileEndpointsShardedClusters())));

    monitors.add(createWatcher(
        Pod.class,
        PodList.class,
        onDelete(
            reconcilePodClusters()
            .andThen(reconcilePodDistributedLogs()))));
  }

  private <T extends CustomResource<?, ?>,
      L extends DefaultKubernetesResourceList<T>> WatcherMonitor<T> createCustomResourceWatcher(
      @NotNull Class<T> crClass, @NotNull Class<L> listClass,
      @NotNull BiConsumer<Action, T> consumer) {

    return new WatcherMonitor<>(crClass.getSimpleName(),
        watcherListener -> client
        .resources(crClass, listClass)
        .inAnyNamespace()
        .watch(watcherFactory.createWatcher(consumer, watcherListener)));
  }

  private <T extends HasMetadata,
      L extends KubernetesResourceList<T>> WatcherMonitor<T> createWatcher(
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

  private <T> BiConsumer<Action, T> onDelete(BiConsumer<Action, T> consumer) {
    return (action, resource) -> {
      if (action == Action.DELETED) {
        consumer.accept(action, resource);
      }
    };
  }

  private BiConsumer<Action, StackGresConfig> reconcileConfig() {
    return (action, config) -> configReconciliatorCycle.reconcile(config);
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
            cluster.getSpec().getSgInstanceProfile(),
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
            cluster.getSpec().getConfigurations().getSgPostgresConfig(),
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
            cluster.getSpec().getConfigurations().getSgPoolingConfig(),
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
            Optional.ofNullable(cluster.getSpec().getConfigurations().getBackups())
            .orElse(List.of())
            .stream().findFirst().map(StackGresClusterBackupConfiguration::getSgObjectStorage)
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
            distributedLogs.getSpec().getSgInstanceProfile(),
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
            distributedLogs.getSpec().getConfigurations().getSgPostgresConfig(),
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
            shardedCluster.getSpec().getCoordinator().getSgInstanceProfile(),
            instanceProfile.getMetadata().getName())
            || Objects.equals(
                shardedCluster.getSpec().getShards().getSgInstanceProfile(),
                instanceProfile.getMetadata().getName())
            || Optional.ofNullable(shardedCluster.getSpec()
                .getShards().getOverrides()).orElse(List.of())
            .stream().anyMatch(spec -> Objects
                .equals(
                    spec.getSgInstanceProfile(),
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
            shardedCluster.getSpec().getCoordinator().getConfigurations().getSgPostgresConfig(),
            postgresConfig.getMetadata().getName())
            || Objects.equals(
                shardedCluster.getSpec().getShards().getConfigurations().getSgPostgresConfig(),
                postgresConfig.getMetadata().getName())
            || Optional.ofNullable(shardedCluster.getSpec()
                .getShards().getOverrides()).orElse(List.of())
            .stream().anyMatch(spec -> spec.getConfigurations() != null
                && Objects.equals(
                    spec.getConfigurations().getSgPostgresConfig(),
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
            .getCoordinator().getConfigurations().getSgPoolingConfig(),
            poolingConfig.getMetadata().getName())
            || Objects.equals(
                shardedCluster.getSpec()
                .getShards().getConfigurations().getSgPoolingConfig(),
                poolingConfig.getMetadata().getName())
            || Optional.ofNullable(shardedCluster.getSpec().getShards().getOverrides())
            .orElse(List.of())
            .stream().anyMatch(spec -> spec.getConfigurations() != null
                && Objects.equals(
                    spec.getConfigurations().getSgPoolingConfig(),
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
            Optional.ofNullable(shardedCluster.getSpec().getConfigurations().getBackups())
            .orElse(List.of())
            .stream().findFirst()
            .map(StackGresShardedClusterBackupConfiguration::getSgObjectStorage)
            .orElse(null),
            objectStorage.getMetadata().getName()))
        .forEach(shardedCluster -> reconcileShardedCluster().accept(action, shardedCluster));
  }

  private BiConsumer<Action, Endpoints> reconcileEndpointsShardedClusters() {
    String clusterScopeKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.CLUSTER_SCOPE_KEY;
    return (action, endpoints) -> client
        .resources(StackGresShardedCluster.class, StackGresShardedClusterList.class)
        .inNamespace(endpoints.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(shardedCluster -> endpoints.getMetadata().getLabels() != null)
        .filter(shardedCluster -> Objects.equals(
            endpoints.getMetadata().getLabels().get(clusterScopeKey),
            shardedCluster.getMetadata().getName()))
        .forEach(shardedCluster -> reconcileShardedCluster().accept(action, shardedCluster));
  }

  private BiConsumer<Action, Pod> reconcilePodClusters() {
    String clusterNameKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.CLUSTER_NAME_KEY;
    return (action, pod) -> client
        .resources(StackGresCluster.class, StackGresClusterList.class)
        .inNamespace(pod.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(cluster -> pod.getMetadata().getLabels() != null)
        .filter(cluster -> Objects.equals(
            pod.getMetadata().getLabels().get(clusterNameKey),
            cluster.getMetadata().getName()))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, Pod> reconcilePodDistributedLogs() {
    String distributedLogsNameKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.DISTRIBUTED_LOGS_CLUSTER_NAME_KEY;
    return (action, pod) -> client
        .resources(StackGresDistributedLogs.class, StackGresDistributedLogsList.class)
        .inNamespace(pod.getMetadata().getNamespace())
        .list()
        .getItems()
        .stream()
        .filter(distributedLogs -> pod.getMetadata().getLabels() != null)
        .filter(distributedLogs -> Objects.equals(
            pod.getMetadata().getLabels().get(distributedLogsNameKey),
            distributedLogs.getMetadata().getName()))
        .forEach(distributedLogs -> reconcileDistributedLogs().accept(action, distributedLogs));
  }

  @Override
  public void stopWatchers() {
    monitors.forEach(WatcherMonitor::close);
  }

}
