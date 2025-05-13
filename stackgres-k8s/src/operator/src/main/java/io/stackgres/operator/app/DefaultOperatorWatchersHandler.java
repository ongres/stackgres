/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
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
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptList;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupList;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterList;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsList;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamList;
import io.stackgres.operator.common.ResourceWatcherFactory;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.backup.BackupReconciliator;
import io.stackgres.operator.conciliation.cluster.ClusterReconciliator;
import io.stackgres.operator.conciliation.config.ConfigReconciliator;
import io.stackgres.operator.conciliation.dbops.DbOpsReconciliator;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsReconciliator;
import io.stackgres.operator.conciliation.shardedbackup.ShardedBackupReconciliator;
import io.stackgres.operator.conciliation.shardedcluster.ShardedClusterReconciliator;
import io.stackgres.operator.conciliation.shardeddbops.ShardedDbOpsReconciliator;
import io.stackgres.operator.conciliation.stream.StreamReconciliator;
import io.stackgres.operatorframework.resource.WatcherMonitor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class DefaultOperatorWatchersHandler implements OperatorWatchersHandler {

  private final List<String> allowedNamespaces = OperatorProperty.getAllowedNamespaces();

  private final List<WatcherMonitor<?>> monitors = new ArrayList<>();

  private final KubernetesClient client;
  private final ConfigReconciliator configReconciliatorCycle;
  private final ClusterReconciliator clusterReconciliatorCycle;
  private final DistributedLogsReconciliator distributedLogsReconciliatorCycle;
  private final DbOpsReconciliator dbOpsReconciliatorCycle;
  private final BackupReconciliator backupReconciliatorCycle;
  private final ShardedClusterReconciliator shardedClusterReconciliatorCycle;
  private final ShardedBackupReconciliator shardedBackupReconciliatorCycle;
  private final ShardedDbOpsReconciliator shardedDbOpsReconciliatorCycle;
  private final StreamReconciliator streamReconciliatorCycle;
  private final ResourceWatcherFactory watcherFactory;
  private final Map<String, StackGresConfig> configs =
      Collections.synchronizedMap(new HashMap<>());
  private final Map<String, StackGresCluster> clusters =
      Collections.synchronizedMap(new HashMap<>());
  private final Map<String, StackGresDistributedLogs> distributedLogs =
      Collections.synchronizedMap(new HashMap<>());
  private final Map<String, StackGresBackup> backups =
      Collections.synchronizedMap(new HashMap<>());
  private final Map<String, StackGresDbOps> dbOps =
      Collections.synchronizedMap(new HashMap<>());
  private final Map<String, StackGresShardedCluster> shardedClusters =
      Collections.synchronizedMap(new HashMap<>());
  private final Map<String, StackGresShardedBackup> shardedBackups =
      Collections.synchronizedMap(new HashMap<>());
  private final Map<String, StackGresShardedDbOps> shardedDbOps =
      Collections.synchronizedMap(new HashMap<>());
  private final Map<String, StackGresStream> streams =
      Collections.synchronizedMap(new HashMap<>());
  private final DeployedResourcesCache deployedResourcesCache;

  @Inject
  public DefaultOperatorWatchersHandler(
      KubernetesClient client,
      ConfigReconciliator configReconciliatorCycle,
      ClusterReconciliator clusterReconciliatorCycle,
      DistributedLogsReconciliator distributedLogsReconciliatorCycle,
      DbOpsReconciliator dbOpsReconciliatorCycle,
      BackupReconciliator backupReconciliatorCycle,
      ShardedClusterReconciliator shardedClusterReconciliatorCycle,
      ShardedBackupReconciliator shardedBackupReconciliatorCycle,
      ShardedDbOpsReconciliator shardedDbOpsReconciliatorCycle,
      StreamReconciliator streamReconciliatorCycle,
      ResourceWatcherFactory watcherFactory,
      DeployedResourcesCache deployedResourcesCache) {
    this.client = client;
    this.configReconciliatorCycle = configReconciliatorCycle;
    this.clusterReconciliatorCycle = clusterReconciliatorCycle;
    this.distributedLogsReconciliatorCycle = distributedLogsReconciliatorCycle;
    this.dbOpsReconciliatorCycle = dbOpsReconciliatorCycle;
    this.backupReconciliatorCycle = backupReconciliatorCycle;
    this.shardedClusterReconciliatorCycle = shardedClusterReconciliatorCycle;
    this.shardedBackupReconciliatorCycle = shardedBackupReconciliatorCycle;
    this.shardedDbOpsReconciliatorCycle = shardedDbOpsReconciliatorCycle;
    this.streamReconciliatorCycle = streamReconciliatorCycle;
    this.watcherFactory = watcherFactory;
    this.deployedResourcesCache = deployedResourcesCache;
  }

  @Override
  public void startWatchers() {
    monitors.addAll(createCustomResourceWatchers(
        StackGresConfig.class,
        StackGresConfigList.class,
        onCreateOrUpdateAndOnDelete(
            reconcileConfig()
            .andThen(putConfig()),
            invalidateConfig()
            .andThen(removeConfig()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresCluster.class,
        StackGresClusterList.class,
        onCreateOrUpdateAndOnDelete(
            putCluster()
            .andThen(reconcileCluster()),
            invalidateCluster()
            .andThen(removeCluster()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresProfile.class,
        StackGresProfileList.class,
        onCreateOrUpdate(
            reconcileInstanceProfileClusters()
            .andThen(reconcileInstanceProfileDistributedLogs())
            .andThen(reconcileInstanceProfileShardedClusters()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresPostgresConfig.class,
        StackGresPostgresConfigList.class,
        onCreateOrUpdate(
            reconcilePostgresConfigClusters()
            .andThen(reconcilePostgresConfigDistributedLogs())
            .andThen(reconcilePostgresConfigShardedClusters()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresPoolingConfig.class,
        StackGresPoolingConfigList.class,
        onCreateOrUpdate(reconcilePoolingConfigClusters()
            .andThen(reconcilePoolingConfigShardedClusters()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresObjectStorage.class,
        StackGresObjectStorageList.class,
        onCreateOrUpdate(
            reconcileObjectStorageClusters()
            .andThen(reconcileObjectStorageShardedClusters()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresScript.class,
        StackGresScriptList.class,
        onCreateOrUpdate(
            reconcileScriptClusters()
            .andThen(reconcileScriptShardedClusters()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresBackup.class,
        StackGresBackupList.class,
        onCreateOrUpdateAndOnDelete(
            putBackup()
            .andThen(reconcileBackup()),
            invalidateBackup()
            .andThen(removeBackup()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresDbOps.class,
        StackGresDbOpsList.class,
        onCreateOrUpdateAndOnDelete(
            putDbOps()
            .andThen(reconcileDbOps()),
            invalidateDbOps()
            .andThen(removeDbOps()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresDistributedLogs.class,
        StackGresDistributedLogsList.class,
        onCreateOrUpdateAndOnDelete(
            putDistributedLogs()
            .andThen(reconcileDistributedLogs()),
            invalidateDistributedLogs()
            .andThen(removeDistributedLogs()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresShardedCluster.class,
        StackGresShardedClusterList.class,
        onCreateOrUpdateAndOnDelete(
            putShardedCluster()
            .andThen(reconcileShardedCluster()),
            invalidateShardedCluster()
            .andThen(removeShardedCluster()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresShardedBackup.class,
        StackGresShardedBackupList.class,
        onCreateOrUpdateAndOnDelete(
            putShardedBackup()
            .andThen(reconcileShardedBackup()),
            invalidateShardedBackup()
            .andThen(removeShardedBackup()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresShardedDbOps.class,
        StackGresShardedDbOpsList.class,
        onCreateOrUpdateAndOnDelete(
            putShardedDbOps()
            .andThen(reconcileShardedDbOps()),
            invalidateShardedDbOps()
            .andThen(removeShardedDbOps()))));

    monitors.addAll(createCustomResourceWatchers(
        StackGresStream.class,
        StackGresStreamList.class,
        onCreateOrUpdateAndOnDelete(
            putStream()
            .andThen(reconcileStream()),
            invalidateStream()
            .andThen(removeStream()))));

    monitors.addAll(createWatchers(
        Endpoints.class,
        EndpointsList.class,
        onCreateOrUpdate(
            reconcileEndpointsShardedClusters())));

    monitors.addAll(createWatchers(
        Pod.class,
        PodList.class,
        onCreateOrUpdateOrDelete(
            reconcilePodClusters()
            .andThen(reconcilePodDistributedLogs())
            .andThen(reconcilePodBackups())
            .andThen(reconcilePodDbOps())
            .andThen(reconcilePodShardedBackups())
            .andThen(reconcilePodShardedDbOps())
            .andThen(reconcilePodStreams()))));

    monitors.addAll(createWatchers(
        PersistentVolumeClaim.class,
        PersistentVolumeClaimList.class,
        onCreateOrUpdateOrDelete(
            reconcilePvcClusters())));
  }

  private <T extends CustomResource<?, ?>,
      L extends DefaultKubernetesResourceList<T>> List<WatcherMonitor<T>> createCustomResourceWatchers(
      @NotNull Class<T> crClass, @NotNull Class<L> listClass,
      @NotNull BiConsumer<Action, T> consumer) {

    if (!allowedNamespaces.isEmpty()) {
      return allowedNamespaces.stream()
          .<WatcherMonitor<T>>map(allowedNamespace -> new WatcherMonitor<>(crClass.getSimpleName(),
              watcherListener -> client
              .resources(crClass, listClass)
              .inNamespace(allowedNamespace)
              .watch(watcherFactory.createWatcher(consumer, watcherListener))))
          .toList();
    }

    return List.of(new WatcherMonitor<>(crClass.getSimpleName(),
        watcherListener -> client
        .resources(crClass, listClass)
        .inAnyNamespace()
        .watch(watcherFactory.createWatcher(consumer, watcherListener))));
  }

  private <T extends HasMetadata,
      L extends KubernetesResourceList<T>> List<WatcherMonitor<T>> createWatchers(
      @NotNull Class<T> crClass, @NotNull Class<L> listClass,
      @NotNull BiConsumer<Action, T> consumer) {

    if (!allowedNamespaces.isEmpty()) {
      return allowedNamespaces.stream()
          .<WatcherMonitor<T>>map(allowedNamespace -> new WatcherMonitor<>(crClass.getSimpleName(),
              watcherListener -> client
              .resources(crClass, listClass)
              .inNamespace(allowedNamespace)
              .watch(watcherFactory.createWatcher(consumer, watcherListener))))
          .toList();
    }

    return List.of(new WatcherMonitor<>(crClass.getSimpleName(),
        watcherListener -> client
        .resources(crClass, listClass)
        .inAnyNamespace()
        .watch(watcherFactory.createWatcher(consumer, watcherListener))));
  }

  private <T> BiConsumer<Action, T> onCreateOrUpdate(BiConsumer<Action, T> consumer) {
    return (action, resource) -> {
      if (action == Action.ADDED || action == Action.MODIFIED) {
        consumer.accept(action, resource);
      }
    };
  }

  private <T> BiConsumer<Action, T> onCreateOrUpdateAndOnDelete(
      BiConsumer<Action, T> consumerOnCreateOrUpdate,
      BiConsumer<Action, T> consumerOnDelete) {
    return (action, resource) -> {
      if (action == Action.ADDED || action == Action.MODIFIED) {
        consumerOnCreateOrUpdate.accept(action, resource);
      }
      if (action == Action.DELETED) {
        consumerOnDelete.accept(action, resource);
      }
    };
  }

  private <T> BiConsumer<Action, T> onCreateOrUpdateOrDelete(BiConsumer<Action, T> consumer) {
    return (action, resource) -> {
      if (action == Action.ADDED || action == Action.MODIFIED || action == Action.DELETED) {
        consumer.accept(action, resource);
      }
    };
  }

  private String resourceId(HasMetadata resource) {
    return resource.getMetadata().getNamespace() + "." + resource.getMetadata().getName();
  }

  private BiConsumer<Action, StackGresConfig> putConfig() {
    return (action, config) -> configs.put(resourceId(config), config);
  }

  private BiConsumer<Action, StackGresCluster> putCluster() {
    return (action, cluster) -> clusters.put(resourceId(cluster), cluster);
  }

  private BiConsumer<Action, StackGresDistributedLogs> putDistributedLogs() {
    return (action, distributedLogs) -> this.distributedLogs
        .put(resourceId(distributedLogs), distributedLogs);
  }

  private BiConsumer<Action, StackGresBackup> putBackup() {
    return (action, backup) -> backups.put(resourceId(backup), backup);
  }

  private BiConsumer<Action, StackGresDbOps> putDbOps() {
    return (action, dbOps) -> this.dbOps.put(resourceId(dbOps), dbOps);
  }

  private BiConsumer<Action, StackGresShardedCluster> putShardedCluster() {
    return (action, cluster) -> shardedClusters.put(resourceId(cluster), cluster);
  }

  private BiConsumer<Action, StackGresShardedBackup> putShardedBackup() {
    return (action, backup) -> shardedBackups.put(resourceId(backup), backup);
  }

  private BiConsumer<Action, StackGresShardedDbOps> putShardedDbOps() {
    return (action, dbOps) -> shardedDbOps.put(resourceId(dbOps), dbOps);
  }

  private BiConsumer<Action, StackGresStream> putStream() {
    return (action, stream) -> this.streams.put(resourceId(stream), stream);
  }

  private BiConsumer<Action, StackGresConfig> removeConfig() {
    return (action, config) -> configs.remove(resourceId(config));
  }

  private BiConsumer<Action, StackGresCluster> removeCluster() {
    return (action, cluster) -> clusters.remove(resourceId(cluster));
  }

  private BiConsumer<Action, StackGresDistributedLogs> removeDistributedLogs() {
    return (action, distributedLogs) -> this.distributedLogs
        .remove(resourceId(distributedLogs));
  }

  private BiConsumer<Action, StackGresBackup> removeBackup() {
    return (action, backup) -> backups.remove(resourceId(backup));
  }

  private BiConsumer<Action, StackGresDbOps> removeDbOps() {
    return (action, dbOps) -> this.dbOps.remove(resourceId(dbOps));
  }

  private BiConsumer<Action, StackGresShardedCluster> removeShardedCluster() {
    return (action, cluster) -> shardedClusters.remove(resourceId(cluster));
  }

  private BiConsumer<Action, StackGresShardedBackup> removeShardedBackup() {
    return (action, backup) -> shardedBackups.remove(resourceId(backup));
  }

  private BiConsumer<Action, StackGresShardedDbOps> removeShardedDbOps() {
    return (action, dbOps) -> shardedDbOps.remove(resourceId(dbOps));
  }

  private BiConsumer<Action, StackGresStream> removeStream() {
    return (action, stream) -> this.streams.remove(resourceId(stream));
  }

  private BiConsumer<Action, StackGresConfig> reconcileConfig() {
    return (action, config) -> Optional
        .ofNullable(configs.get(resourceId(config)))
        .filter(oldConfig -> config.getMetadata().getAnnotations() == null
            || oldConfig == null
            || oldConfig.getMetadata().getAnnotations() == null
            || Objects.equals(
                config.getMetadata().getAnnotations().get(StackGresContext.LOCK_TIMEOUT_KEY),
                oldConfig.getMetadata().getAnnotations().get(StackGresContext.LOCK_TIMEOUT_KEY)))
        .ifPresent(ignore -> configReconciliatorCycle.reconcile(config));
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

  private BiConsumer<Action, StackGresShardedBackup> reconcileShardedBackup() {
    return (action, backup) -> shardedBackupReconciliatorCycle.reconcile(backup);
  }

  private BiConsumer<Action, StackGresShardedDbOps> reconcileShardedDbOps() {
    return (action, dbOps) -> shardedDbOpsReconciliatorCycle.reconcile(dbOps);
  }

  private BiConsumer<Action, StackGresStream> reconcileStream() {
    return (action, stream) -> streamReconciliatorCycle.reconcile(stream);
  }

  private BiConsumer<Action, StackGresProfile> reconcileInstanceProfileClusters() {
    return (action, instanceProfile) -> synchronizedCopyOfValues(clusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            instanceProfile.getMetadata().getNamespace()))
        .filter(cluster -> Objects.equals(
            cluster.getSpec().getSgInstanceProfile(),
            instanceProfile.getMetadata().getName()))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, StackGresPostgresConfig> reconcilePostgresConfigClusters() {
    return (action, postgresConfig) -> synchronizedCopyOfValues(clusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            postgresConfig.getMetadata().getNamespace()))
        .filter(cluster -> Objects.equals(
            cluster.getSpec().getConfigurations().getSgPostgresConfig(),
            postgresConfig.getMetadata().getName()))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, StackGresPoolingConfig> reconcilePoolingConfigClusters() {
    return (action, poolingConfig) -> synchronizedCopyOfValues(clusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            poolingConfig.getMetadata().getNamespace()))
        .filter(cluster -> Objects.equals(
            cluster.getSpec().getConfigurations().getSgPoolingConfig(),
            poolingConfig.getMetadata().getName()))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, StackGresObjectStorage> reconcileObjectStorageClusters() {
    return (action, objectStorage) -> synchronizedCopyOfValues(clusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            objectStorage.getMetadata().getNamespace()))
        .filter(cluster -> Objects.equals(
            Optional.ofNullable(cluster.getSpec().getConfigurations().getBackups())
            .orElse(List.of())
            .stream().findFirst().map(StackGresClusterBackupConfiguration::getSgObjectStorage)
            .orElse(null),
            objectStorage.getMetadata().getName()))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, StackGresScript> reconcileScriptClusters() {
    return (action, script) -> synchronizedCopyOfValues(clusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            script.getMetadata().getNamespace()))
        .filter(cluster -> Optional.ofNullable(cluster.getSpec().getManagedSql())
            .map(StackGresClusterManagedSql::getScripts)
            .stream()
            .flatMap(List::stream)
            .map(StackGresClusterManagedScriptEntry::getSgScript)
            .anyMatch(script.getMetadata().getName()::equals))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, StackGresProfile> reconcileInstanceProfileDistributedLogs() {
    return (action, instanceProfile) -> synchronizedCopyOfValues(distributedLogs)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            instanceProfile.getMetadata().getNamespace()))
        .filter(distributedLogs -> Objects.equals(
            distributedLogs.getSpec().getSgInstanceProfile(),
            instanceProfile.getMetadata().getName()))
        .forEach(distributedLogs -> reconcileDistributedLogs().accept(action, distributedLogs));
  }

  private BiConsumer<Action, StackGresPostgresConfig> reconcilePostgresConfigDistributedLogs() {
    return (action, postgresConfig) -> synchronizedCopyOfValues(distributedLogs)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            postgresConfig.getMetadata().getNamespace()))
        .filter(distributedLogs -> Objects.equals(
            distributedLogs.getSpec().getConfigurations().getSgPostgresConfig(),
            postgresConfig.getMetadata().getName()))
        .forEach(distributedLogs -> reconcileDistributedLogs().accept(action, distributedLogs));
  }

  private BiConsumer<Action, StackGresProfile> reconcileInstanceProfileShardedClusters() {
    return (action, instanceProfile) -> synchronizedCopyOfValues(shardedClusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            instanceProfile.getMetadata().getNamespace()))
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
    return (action, postgresConfig) -> synchronizedCopyOfValues(shardedClusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            postgresConfig.getMetadata().getNamespace()))
        .filter(shardedCluster -> {
          return Objects.equals(
              Optional.ofNullable(
                  shardedCluster.getSpec().getCoordinator().getConfigurationsForCoordinator())
              .map(StackGresClusterConfigurations::getSgPostgresConfig),
              Optional.of(postgresConfig.getMetadata().getName()))
              || Objects.equals(
                  Optional.ofNullable(
                      shardedCluster.getSpec().getShards().getConfigurations())
                  .map(StackGresClusterConfigurations::getSgPostgresConfig),
                  Optional.of(postgresConfig.getMetadata().getName()))
              || Optional.ofNullable(shardedCluster.getSpec().getShards().getOverrides())
                  .orElse(List.of())
                  .stream().anyMatch(spec -> spec.getConfigurationsForShards() != null
                      && Objects.equals(
                          spec.getConfigurationsForShards().getSgPostgresConfig(),
                          postgresConfig.getMetadata().getName()));
        })
        .forEach(shardedCluster -> reconcileShardedCluster().accept(action, shardedCluster));
  }

  private BiConsumer<Action, StackGresPoolingConfig> reconcilePoolingConfigShardedClusters() {
    return (action, poolingConfig) -> synchronizedCopyOfValues(shardedClusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            poolingConfig.getMetadata().getNamespace()))
        .filter(shardedCluster -> Objects.equals(
            Optional.ofNullable(
                shardedCluster.getSpec().getCoordinator().getConfigurationsForCoordinator())
            .map(StackGresClusterConfigurations::getSgPoolingConfig),
            Optional.of(poolingConfig.getMetadata().getName()))
            || Objects.equals(
                Optional.ofNullable(
                    shardedCluster.getSpec().getShards().getConfigurations())
                .map(StackGresClusterConfigurations::getSgPoolingConfig),
                Optional.of(poolingConfig.getMetadata().getName()))
            || Optional.ofNullable(shardedCluster.getSpec().getShards().getOverrides())
                .orElse(List.of())
                .stream().anyMatch(spec -> spec.getConfigurationsForShards() != null
                    && Objects.equals(
                        spec.getConfigurationsForShards().getSgPoolingConfig(),
                        poolingConfig.getMetadata().getName())))
        .forEach(shardedCluster -> reconcileShardedCluster().accept(action, shardedCluster));
  }

  private BiConsumer<Action, StackGresObjectStorage> reconcileObjectStorageShardedClusters() {
    return (action, objectStorage) -> synchronizedCopyOfValues(shardedClusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            objectStorage.getMetadata().getNamespace()))
        .filter(shardedCluster -> Objects.equals(
            Optional.ofNullable(shardedCluster.getSpec().getConfigurations())
            .map(StackGresShardedClusterConfigurations::getBackups)
            .orElse(List.of())
            .stream().findFirst()
            .map(StackGresShardedClusterBackupConfiguration::getSgObjectStorage)
            .orElse(null),
            objectStorage.getMetadata().getName()))
        .forEach(shardedCluster -> reconcileShardedCluster().accept(action, shardedCluster));
  }

  private BiConsumer<Action, StackGresScript> reconcileScriptShardedClusters() {
    return (action, script) -> synchronizedCopyOfValues(shardedClusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            script.getMetadata().getNamespace()))
        .filter(cluster -> Optional.ofNullable(cluster.getSpec().getCoordinator().getManagedSql())
            .map(StackGresClusterManagedSql::getScripts)
            .stream()
            .flatMap(List::stream)
            .map(StackGresClusterManagedScriptEntry::getSgScript)
            .anyMatch(script.getMetadata().getName()::equals)
            || Optional.ofNullable(cluster.getSpec().getShards().getManagedSql())
            .map(StackGresClusterManagedSql::getScripts)
            .stream()
            .flatMap(List::stream)
            .map(StackGresClusterManagedScriptEntry::getSgScript)
            .anyMatch(script.getMetadata().getName()::equals)
            || Optional.ofNullable(cluster.getSpec().getShards().getOverrides())
            .stream()
            .flatMap(List::stream)
            .flatMap(override -> Optional.ofNullable(override.getManagedSql())
                .map(StackGresClusterManagedSql::getScripts)
                .stream())
            .flatMap(List::stream)
            .map(StackGresClusterManagedScriptEntry::getSgScript)
            .anyMatch(script.getMetadata().getName()::equals))
        .forEach(cluster -> reconcileShardedCluster().accept(action, cluster));
  }

  private BiConsumer<Action, Endpoints> reconcileEndpointsShardedClusters() {
    String clusterScopeKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.CLUSTER_SCOPE_KEY;
    return (action, endpoints) -> synchronizedCopyOfValues(shardedClusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            endpoints.getMetadata().getNamespace()))
        .filter(shardedCluster -> endpoints.getMetadata().getLabels() != null)
        .filter(shardedCluster -> Objects.equals(
            endpoints.getMetadata().getLabels().get(clusterScopeKey),
            shardedCluster.getMetadata().getName()))
        .forEach(shardedCluster -> reconcileShardedCluster().accept(action, shardedCluster));
  }

  private BiConsumer<Action, Pod> reconcilePodClusters() {
    String clusterNameKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.CLUSTER_NAME_KEY;
    return (action, pod) -> synchronizedCopyOfValues(clusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            pod.getMetadata().getNamespace()))
        .filter(cluster -> pod.getMetadata().getLabels() != null)
        .filter(cluster -> Objects.equals(
            pod.getMetadata().getLabels().get(clusterNameKey),
            cluster.getMetadata().getName()))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, Pod> reconcilePodDistributedLogs() {
    String distributedLogsNameKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.DISTRIBUTED_LOGS_CLUSTER_NAME_KEY;
    return (action, pod) -> synchronizedCopyOfValues(distributedLogs)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            pod.getMetadata().getNamespace()))
        .filter(distributedLogs -> pod.getMetadata().getLabels() != null)
        .filter(distributedLogs -> Objects.equals(
            pod.getMetadata().getLabels().get(distributedLogsNameKey),
            distributedLogs.getMetadata().getName()))
        .forEach(distributedLogs -> reconcileDistributedLogs().accept(action, distributedLogs));
  }

  private BiConsumer<Action, Pod> reconcilePodBackups() {
    String clusterNameKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.CLUSTER_NAME_KEY;
    return (action, pod) -> synchronizedCopyOfValues(backups)
        .stream()
        .filter(backup -> Objects.equals(
            backup.getMetadata().getNamespace(),
            pod.getMetadata().getNamespace()))
        .filter(backup -> pod.getMetadata().getLabels() != null)
        .forEach(backup -> synchronizedCopyOfValues(clusters)
            .stream()
            .filter(cluster -> Objects.equals(
                backup.getMetadata().getNamespace(),
                cluster.getMetadata().getNamespace()))
            .filter(cluster -> Objects.equals(
                backup.getSpec().getSgCluster(),
                cluster.getMetadata().getName()))
            .filter(cluster -> Objects.equals(
                pod.getMetadata().getLabels().get(clusterNameKey),
                cluster.getMetadata().getName()))
            .forEach(cluster -> reconcileBackup().accept(action, backup)));
  }

  private BiConsumer<Action, Pod> reconcilePodDbOps() {
    String clusterNameKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.CLUSTER_NAME_KEY;
    return (action, pod) -> synchronizedCopyOfValues(dbOps)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            pod.getMetadata().getNamespace()))
        .filter(dbOps -> pod.getMetadata().getLabels() != null)
        .forEach(dbOps -> synchronizedCopyOfValues(clusters)
            .stream()
            .filter(cluster -> Objects.equals(
                dbOps.getMetadata().getNamespace(),
                cluster.getMetadata().getNamespace()))
            .filter(cluster -> Objects.equals(
                dbOps.getSpec().getSgCluster(),
                cluster.getMetadata().getName()))
            .filter(cluster -> Objects.equals(
                pod.getMetadata().getLabels().get(clusterNameKey),
                cluster.getMetadata().getName()))
            .forEach(cluster -> reconcileDbOps().accept(action, dbOps)));
  }

  private BiConsumer<Action, Pod> reconcilePodShardedBackups() {
    String clusterNameKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.SHARDED_CLUSTER_NAME_KEY;
    return (action, pod) -> synchronizedCopyOfValues(shardedBackups)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            pod.getMetadata().getNamespace()))
        .filter(backup -> pod.getMetadata().getLabels() != null)
        .forEach(backup -> synchronizedCopyOfValues(shardedClusters)
            .stream()
            .filter(cluster -> Objects.equals(
                backup.getMetadata().getNamespace(),
                cluster.getMetadata().getNamespace()))
            .filter(cluster -> Objects.equals(
                backup.getSpec().getSgShardedCluster(),
                cluster.getMetadata().getName()))
            .filter(cluster -> Objects.equals(
                pod.getMetadata().getLabels().get(clusterNameKey),
                cluster.getMetadata().getName()))
            .forEach(cluster -> reconcileShardedBackup().accept(action, backup)));
  }

  private BiConsumer<Action, Pod> reconcilePodShardedDbOps() {
    String clusterNameKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.SHARDED_CLUSTER_NAME_KEY;
    return (action, pod) -> synchronizedCopyOfValues(shardedDbOps)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            pod.getMetadata().getNamespace()))
        .filter(dbOps -> pod.getMetadata().getLabels() != null)
        .forEach(dbOps -> synchronizedCopyOfValues(shardedClusters)
            .stream()
            .filter(cluster -> Objects.equals(
                dbOps.getMetadata().getNamespace(),
                cluster.getMetadata().getNamespace()))
            .filter(cluster -> Objects.equals(
                dbOps.getSpec().getSgShardedCluster(),
                cluster.getMetadata().getName()))
            .filter(cluster -> Objects.equals(
                pod.getMetadata().getLabels().get(clusterNameKey),
                cluster.getMetadata().getName()))
            .forEach(cluster -> reconcileShardedDbOps().accept(action, dbOps)));
  }

  private BiConsumer<Action, Pod> reconcilePodStreams() {
    String streamNameKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.STREAM_NAME_KEY;
    return (action, pod) -> synchronizedCopyOfValues(streams)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            pod.getMetadata().getNamespace()))
        .filter(stream -> pod.getMetadata().getLabels() != null)
        .filter(stream -> Objects.equals(
            pod.getMetadata().getLabels().get(streamNameKey),
            stream.getMetadata().getName()))
        .forEach(stream -> reconcileStream().accept(action, stream));
  }

  private BiConsumer<Action, PersistentVolumeClaim> reconcilePvcClusters() {
    String clusterNameKey =
        StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.CLUSTER_NAME_KEY;
    return (action, pod) -> synchronizedCopyOfValues(clusters)
        .stream()
        .filter(cluster -> Objects.equals(
            cluster.getMetadata().getNamespace(),
            pod.getMetadata().getNamespace()))
        .filter(cluster -> pod.getMetadata().getLabels() != null)
        .filter(cluster -> Objects.equals(
            pod.getMetadata().getLabels().get(clusterNameKey),
            cluster.getMetadata().getName()))
        .forEach(cluster -> reconcileCluster().accept(action, cluster));
  }

  private BiConsumer<Action, StackGresConfig> invalidateConfig() {
    return (action, config) ->  deployedResourcesCache.removeAll(config);
  }

  private BiConsumer<Action, StackGresCluster> invalidateCluster() {
    return (action, cluster) -> deployedResourcesCache.removeAll(cluster);
  }

  private BiConsumer<Action, StackGresDistributedLogs> invalidateDistributedLogs() {
    return (action, distributedlogs) -> deployedResourcesCache.removeAll(distributedlogs);
  }

  private BiConsumer<Action, StackGresShardedCluster> invalidateShardedCluster() {
    return (action, shardedCluster) -> deployedResourcesCache.removeAll(shardedCluster);
  }

  private BiConsumer<Action, StackGresDbOps> invalidateDbOps() {
    return (action, dbops) -> deployedResourcesCache.removeAll(dbops);
  }

  private BiConsumer<Action, StackGresBackup> invalidateBackup() {
    return (action, backup) -> deployedResourcesCache.removeAll(backup);
  }

  private BiConsumer<Action, StackGresShardedBackup> invalidateShardedBackup() {
    return (action, backup) -> deployedResourcesCache.removeAll(backup);
  }

  private BiConsumer<Action, StackGresShardedDbOps> invalidateShardedDbOps() {
    return (action, dbOps) -> deployedResourcesCache.removeAll(dbOps);
  }

  private BiConsumer<Action, StackGresStream> invalidateStream() {
    return (action, stream) -> deployedResourcesCache.removeAll(stream);
  }

  @Override
  public void stopWatchers() {
    monitors.forEach(WatcherMonitor::close);
  }

  private <T> List<T> synchronizedCopyOfValues(Map<?, T> map) {
    synchronized (map) {
      return List.copyOf(map.values());
    }
  }

}
