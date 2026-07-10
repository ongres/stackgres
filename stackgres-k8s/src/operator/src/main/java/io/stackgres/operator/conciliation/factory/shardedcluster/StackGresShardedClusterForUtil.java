/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.CustomServicePortBuilder;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServicesBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfigurationBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurationsPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialDataBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresExporter;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresExporterQueries;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromExternal;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromStorage;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackupBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestorePitrBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresCoordinatorServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresWorkersServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import org.jooq.lambda.Seq;

public abstract class StackGresShardedClusterForUtil implements StackGresShardedClusterUtil {

  public static StackGresCluster getCoordinatorCluster(
      StackGresShardedCluster cluster,
      Optional<StackGresShardedCluster> replicateCluster) {
    switch (StackGresShardingType.fromString(cluster.getSpec().getType())) {
      case CITUS:
        return StackGresShardedClusterForCitusUtil.getCoordinatorCluster(cluster, replicateCluster);
      case DDP:
        return StackGresShardedClusterForDdpUtil.getCoordinatorCluster(cluster, replicateCluster);
      case SHARDING_SPHERE:
        return StackGresShardedClusterForShardingSphereUtil.getCoordinatorCluster(cluster, replicateCluster);
      default:
        throw new UnsupportedOperationException(
            "Sharding technology " + cluster.getSpec().getType() + " not implemented");
    }
  }

  public static StackGresCluster getWorkerCluster(
      StackGresShardedCluster cluster,
      int index,
      Optional<StackGresShardedCluster> replicateCluster) {
    switch (StackGresShardingType.fromString(cluster.getSpec().getType())) {
      case CITUS:
        return StackGresShardedClusterForCitusUtil.getWorkerCluster(cluster, index, replicateCluster);
      case DDP:
        return StackGresShardedClusterForDdpUtil.getWorkerCluster(cluster, index, replicateCluster);
      case SHARDING_SPHERE:
        return StackGresShardedClusterForShardingSphereUtil.getWorkerCluster(cluster, index, replicateCluster);
      default:
        throw new UnsupportedOperationException(
            "Sharding technology " + cluster.getSpec().getType() + " not implemented");
    }
  }

  public static StackGresCluster getQueryRouterCluster(
      StackGresShardedCluster cluster,
      int index,
      Optional<StackGresShardedCluster> replicateCluster) {
    switch (StackGresShardingType.fromString(cluster.getSpec().getType())) {
      case CITUS:
        return StackGresShardedClusterForCitusUtil.getQueryRouterCluster(cluster, index, replicateCluster);
      default:
        throw new UnsupportedOperationException(
            "Sharding technology " + cluster.getSpec().getType() + " not implemented for query routers");
    }
  }

  StackGresCluster getBaseCoordinatorCluster(
      StackGresShardedCluster cluster,
      Optional<StackGresShardedCluster> replicateCluster) {
    final StackGresClusterSpec spec =
        new StackGresClusterSpecBuilder(cluster.getSpec().getCoordinator())
        .withConfigurations(cluster.getSpec().getCoordinator().getConfigurationsForCoordinator())
        .build();
    setClusterSpecFromShardedCluster(cluster, spec, 0, replicateCluster);
    if (cluster.getSpec().getCoordinator().getReplicationForCoordinator() != null) {
      spec.setReplication(cluster.getSpec().getCoordinator().getReplicationForCoordinator());
    } else {
      spec.setReplication(cluster.getSpec().getReplication());
    }
    updateCoordinatorSpec(cluster, spec);
    setShardedPostgresConfig(spec, getCoordinatorPostgresConfig(cluster));
    StackGresCluster coordinatorCluster = new StackGresCluster();
    coordinatorCluster.setMetadata(new ObjectMeta());
    coordinatorCluster.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    coordinatorCluster.getMetadata().setName(
        StackGresShardedClusterUtil.getCoordinatorClusterName(cluster));
    var postgresServices = cluster.getSpec().getPostgresServices();
    spec.setPostgresServices(new StackGresPostgresServicesBuilder()
        .withNewPrimary()
        .withEnabled(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getCoordinator)
            .map(StackGresShardedClusterPostgresCoordinatorServices::getPrimary)
            .map(StackGresPostgresService::getEnabled)
            .orElse(true)
            || Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getCoordinator)
            .map(StackGresShardedClusterPostgresCoordinatorServices::getAny)
            .map(StackGresPostgresService::getEnabled)
            .orElse(true))
        .withCustomPorts(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getCoordinator)
            .map(StackGresShardedClusterPostgresCoordinatorServices::getCustomPorts)
            .map(customPorts -> customPorts
                .stream()
                .map(customPort -> new CustomServicePortBuilder(customPort)
                    .withNodePort(null)
                    .build())
                .toList())
            .orElse(null))
        .endPrimary()
        .withNewReplicas()
        .withEnabled(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getCoordinator)
            .map(StackGresShardedClusterPostgresCoordinatorServices::getAny)
            .map(StackGresPostgresService::getEnabled)
            .orElse(true))
        .withCustomPorts(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getCoordinator)
            .map(StackGresShardedClusterPostgresCoordinatorServices::getCustomPorts)
            .map(customPorts -> customPorts
                .stream()
                .map(customPort -> new CustomServicePortBuilder(customPort)
                    .withNodePort(null)
                    .build())
                .toList())
            .orElse(null))
        .endReplicas()
        .build());
    coordinatorCluster.setSpec(spec);
    return coordinatorCluster;
  }

  abstract void updateCoordinatorSpec(
      StackGresShardedCluster cluster,
      StackGresClusterSpec spec);

  StackGresCluster getBaseWorkerCluster(
      StackGresShardedCluster cluster,
      int index,
      Optional<StackGresShardedCluster> replicateCluster) {
    final StackGresClusterSpec spec =
        new StackGresClusterSpecBuilder(cluster.getSpec().getWorkers())
        .build();
    setClusterSpecFromShardedCluster(cluster, spec, index + 1, replicateCluster);
    if (cluster.getSpec().getWorkers().getReplicationForWorkers() != null) {
      spec.setReplication(cluster.getSpec().getWorkers().getReplicationForWorkers());
    } else {
      spec.setReplication(cluster.getSpec().getReplication());
    }
    spec.setInstances(cluster.getSpec().getWorkers().getInstancesPerCluster());
    cluster.getSpec().getWorkersOverrides()
        .stream()
        .filter(specOverride -> Objects.equals(
            specOverride.getIndex(),
            index))
        .findFirst()
        .ifPresent(specOverride -> setClusterSpecFromShardOverrides(
            cluster, specOverride, spec, index + 1));
    updateWorkerClusterSpec(cluster, spec, index);
    setShardedPostgresConfig(spec, getWorkerPostgresConfig(cluster, index));
    StackGresCluster workersCluster = new StackGresCluster();
    workersCluster.setMetadata(new ObjectMeta());
    workersCluster.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    workersCluster.getMetadata().setName(
        StackGresShardedClusterUtil.getWorkerClusterName(cluster, index));
    var postgresServices = cluster.getSpec().getPostgresServices();
    spec.setPostgresServices(new StackGresPostgresServicesBuilder()
        .withNewPrimary()
        .withEnabled(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getWorkers)
            .map(StackGresShardedClusterPostgresWorkersServices::getPrimaries)
            .map(StackGresPostgresService::getEnabled)
            .orElse(true))
        .withCustomPorts(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getWorkers)
            .map(StackGresShardedClusterPostgresWorkersServices::getCustomPorts)
            .map(customPorts -> customPorts
                .stream()
                .map(customPort -> new CustomServicePortBuilder(customPort)
                    .withNodePort(null)
                    .build())
                .toList())
            .orElse(null))
        .endPrimary()
        .withNewReplicas()
        .withEnabled(false)
        .endReplicas()
        .build());
    workersCluster.setSpec(spec);
    return workersCluster;
  }

  StackGresCluster getBaseQueryRouterCluster(
      StackGresShardedCluster cluster,
      int index,
      Optional<StackGresShardedCluster> replicateCluster) {
    final StackGresClusterSpec spec =
        new StackGresClusterSpecBuilder(cluster.getSpec().getCoordinator())
        .withConfigurations(cluster.getSpec().getCoordinator().getConfigurationsForCoordinator())
        .build();
    setClusterSpecFromShardedCluster(cluster, spec, index + 1, replicateCluster);
    spec.setReplication(null);
    spec.setInstances(1);
    cluster.getSpec().getQueryRoutersOverrides()
        .stream()
        .filter(specOverride -> Objects.equals(
            specOverride.getIndex(),
            index))
        .findFirst()
        .ifPresent(specOverride -> setClusterSpecFromShardOverrides(
            cluster, specOverride, spec, index + 1));
    updateQueryRouterClusterSpec(cluster, spec, index);
    setShardedPostgresConfig(spec, getQueryRouterPostgresConfig(cluster, index));
    StackGresCluster queryRouterCluster = new StackGresCluster();
    queryRouterCluster.setMetadata(new ObjectMeta());
    queryRouterCluster.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    queryRouterCluster.getMetadata().setName(
        StackGresShardedClusterUtil.getQueryRouterClusterName(cluster, index));
    var postgresServices = cluster.getSpec().getPostgresServices();
    spec.setPostgresServices(new StackGresPostgresServicesBuilder()
        .withNewPrimary()
        .withEnabled(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getCoordinator)
            .map(StackGresShardedClusterPostgresCoordinatorServices::getQueryRouters)
            .map(StackGresPostgresService::getEnabled)
            .orElse(true))
        .withCustomPorts(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getCoordinator)
            .map(StackGresShardedClusterPostgresCoordinatorServices::getCustomPorts)
            .map(customPorts -> customPorts
                .stream()
                .map(customPort -> new CustomServicePortBuilder(customPort)
                    .withNodePort(null)
                    .build())
                .toList())
            .orElse(null))
        .endPrimary()
        .withNewReplicas()
        .withEnabled(false)
        .endReplicas()
        .build());
    queryRouterCluster.setSpec(spec);
    return queryRouterCluster;
  }

  void updateWorkerClusterSpec(
      StackGresShardedCluster cluster,
      StackGresClusterSpec spec,
      int index) {
  }

  void updateQueryRouterClusterSpec(
      StackGresShardedCluster cluster,
      StackGresClusterSpec spec,
      int index) {
  }

  // The per-cluster SGPostgresConfig to set on the generated coordinator/worker/query router cluster,
  // or null to keep the configured one. By default the configured SGPostgresConfig is kept as-is;
  // citus overrides these to use the deterministic per-cluster config generated by the operator (see
  // StackGresShardedClusterUtil.coordinatorConfigName/workerConfigName/queryRouterConfigName).
  String getCoordinatorPostgresConfig(StackGresShardedCluster cluster) {
    return null;
  }

  String getWorkerPostgresConfig(StackGresShardedCluster cluster, int index) {
    return null;
  }

  String getQueryRouterPostgresConfig(StackGresShardedCluster cluster, int index) {
    return null;
  }

  private void setShardedPostgresConfig(StackGresClusterSpec spec, String sgPostgresConfig) {
    if (sgPostgresConfig != null && spec.getConfigurations() != null) {
      spec.getConfigurations().setSgPostgresConfig(sgPostgresConfig);
    }
  }

  void setClusterSpecFromShardedCluster(
      StackGresShardedCluster cluster,
      StackGresClusterSpec spec,
      int globalIndex,
      Optional<StackGresShardedCluster> replicateCluster) {
    spec.setProfile(cluster.getSpec().getProfile());
    setPostgres(cluster, spec);
    setPostgresSsl(cluster, spec);
    setPostgresExtensions(cluster, spec);
    setConfigurationsObservability(cluster, spec);
    setConfigurationsPostgresExporter(cluster, spec);
    setConfigurationsBackups(cluster, spec, globalIndex);
    setConfigurationsCredentials(cluster, spec);
    setMetadata(cluster, spec, globalIndex);
    setInitialData(cluster, spec, globalIndex);
    setManagedSql(cluster, spec, globalIndex);
    if (cluster.getSpec().getReplicateFrom() != null) {
      int plainIndex = StackGresShardedClusterUtil.getPlainIndex(cluster, globalIndex);
      spec.setReplicateFrom(new StackGresClusterReplicateFrom());
      if (cluster.getSpec().getReplicateFrom().getInstance() != null) {
        spec.getReplicateFrom().setInstance(new StackGresClusterReplicateFromInstance());
        if (cluster.getSpec().getReplicateFrom().getInstance().getExternal() != null) {
          spec.getReplicateFrom().getInstance().setExternal(new StackGresClusterReplicateFromExternal());
          spec.getReplicateFrom().getInstance().getExternal().setHost(
              cluster.getSpec().getReplicateFrom().getInstance().getExternal().getHosts().get(plainIndex));
          spec.getReplicateFrom().getInstance().getExternal().setPort(
              cluster.getSpec().getReplicateFrom().getInstance().getExternal().getPorts().get(plainIndex));
          if (cluster.getSpec().getReplicateFrom().getInstance().getExternal().getCustomRestoreMethods() != null) {
            spec.getReplicateFrom().getInstance().getExternal().setCustomRestoreMethod(
                cluster.getSpec().getReplicateFrom().getInstance().getExternal().getCustomRestoreMethods()
                .get(plainIndex));
          }
        }
        if (cluster.getSpec().getReplicateFrom().getInstance().getSgShardedCluster() != null) {
          spec.getReplicateFrom().getInstance().setSgCluster(
              StackGresShardedClusterUtil.getClusterName(
                  replicateCluster.orElseThrow(() -> new RuntimeException(
                      "SGShardedCluster "
                      + cluster.getSpec().getReplicateFrom().getInstance().getSgShardedCluster()
                      + " was not found")),
                  globalIndex));
        }
      }
      if (cluster.getSpec().getReplicateFrom().getStorage() != null) {
        spec.getReplicateFrom().setStorage(new StackGresClusterReplicateFromStorage());
        spec.getReplicateFrom().getStorage().setPerformance(
            cluster.getSpec().getReplicateFrom().getStorage().getPerformance());
        spec.getReplicateFrom().getStorage().setSgObjectStorage(
            cluster.getSpec().getReplicateFrom().getStorage().getSgObjectStorage());
        spec.getReplicateFrom().getStorage().setPath(
            cluster.getSpec().getReplicateFrom().getStorage().getPaths().get(plainIndex));
      }
      spec.getReplicateFrom().setUsers(cluster.getSpec().getReplicateFrom().getUsers());
    }
    spec.setDistributedLogs(cluster.getSpec().getDistributedLogs());
    spec.setNonProductionOptions(cluster.getSpec().getNonProductionOptions());
  }

  void setPostgres(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec) {
    spec.setPostgres(
        new StackGresClusterPostgresBuilder(cluster.getSpec().getPostgres())
        .build());
  }

  void setPostgresSsl(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec) {
    if (!Optional.of(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getEnabled)
        .orElse(false)) {
      return;
    }
    spec.setPostgres(
        new StackGresClusterPostgresBuilder(spec.getPostgres())
        .editSsl()
        .withCertificateSecretKeySelector(
            new SecretKeySelector(
                CERTIFICATE_KEY,
                StackGresShardedClusterUtil.postgresSslSecretName(cluster)))
        .withPrivateKeySecretKeySelector(
            new SecretKeySelector(
                PRIVATE_KEY_KEY,
                StackGresShardedClusterUtil.postgresSslSecretName(cluster)))
        .endSsl()
        .build());
  }

  void setPostgresExtensions(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec) {
    spec.getPostgres().setExtensions(
        Optional.ofNullable(cluster.getStatus())
        .map(StackGresShardedClusterStatus::getExtensions)
        .stream()
        .flatMap(List::stream)
        .map(extension -> new StackGresClusterExtensionBuilder()
            .withName(extension.getName())
            .withPublisher(extension.getPublisher())
            .withRepository(extension.getRepository())
            .withVersion(extension.getVersion())
            .build())
        .toList());
  }

  void setConfigurationsObservability(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec) {
    Optional.ofNullable(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getObservability)
        .ifPresent(observability -> {
          if (spec.getConfigurations() == null) {
            spec.setConfigurations(new StackGresClusterConfigurations());
          }
          spec.getConfigurations().setObservability(observability);
        });
  }

  void setConfigurationsPostgresExporter(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec) {
    Optional.ofNullable(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getPostgresExporter)
        .ifPresent(postgresExporter -> {
          if (spec.getConfigurations() == null) {
            spec.setConfigurations(new StackGresClusterConfigurations());
          }
          var queriesFound = Optional.ofNullable(spec.getConfigurations())
              .map(StackGresClusterConfigurations::getPostgresExporter)
              .map(StackGresClusterPostgresExporter::getQueries);
          // Build a fresh exporter per generated sub-cluster instead of storing
          // and mutating the shared SGShardedCluster postgresExporter object.
          // The latter is applied to the coordinator, every shard and the query
          // routers, so mutating it would cross-contaminate their queries and
          // the last write would retroactively rewrite all of them.
          var postgresExporterCopy = new StackGresClusterPostgresExporter();
          postgresExporterCopy.setQueries(
              Optional.ofNullable(postgresExporter.getQueries())
              .map(queries -> queriesFound
                  .map(q -> mergeMap(queries, q))
                  .orElse(queries))
              .or(() -> queriesFound)
              .map(StackGresClusterPostgresExporterQueries::new)
              .orElse(null));
          spec.getConfigurations().setPostgresExporter(postgresExporterCopy);
        });
  }

  void setConfigurationsBackups(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    final int queryRouterIndexOffset = Optional.of(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getQueryRouterIndexOffset)
        .orElse(1024);
    Optional.ofNullable(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getBackups)
        .filter(Predicate.not(List::isEmpty))
        .map(backups -> backups.getFirst())
        .ifPresent(backup -> {
          if (spec.getConfigurations() == null) {
            spec.setConfigurations(new StackGresClusterConfigurations());
          }
          spec.getConfigurations().setBackups(List.of(
              new StackGresClusterBackupConfigurationBuilder()
              .withSgObjectStorage(backup.getSgObjectStorage())
              .withPath(getBackupPath(index, queryRouterIndexOffset, backup))
              .withRetention(backup.getRetention())
              .withCompression(backup.getCompression())
              .withPerformance(backup.getPerformance())
              .withUseVolumeSnapshot(backup.getUseVolumeSnapshot())
              .withVolumeSnapshotClass(backup.getVolumeSnapshotClass())
              .withFastVolumeSnapshot(backup.getFastVolumeSnapshot())
              .withTimeout(backup.getTimeout())
              .withReconciliationTimeout(backup.getReconciliationTimeout())
              .withMaxRetries(backup.getMaxRetries())
              .withRetryDelay(backup.getRetryDelay())
              .withRetryLimit(backup.getRetryLimit())
              .withRetryMaxDelay(backup.getRetryMaxDelay())
              .withRetainWalsForUnmanagedLifecycle(backup.getRetainWalsForUnmanagedLifecycle())
              .build()));
        });
  }

  private String getBackupPath(
      final int index,
      final int queryRouterIndexOffset,
      final StackGresShardedClusterBackupConfiguration backup) {
    String path;
    if (index >= queryRouterIndexOffset + 1) {
      final int queryRouterPathIndex = index - queryRouterIndexOffset - 1;
      if (backup.getQueryRouterPaths() != null && backup.getQueryRouterPaths().size() > queryRouterPathIndex) {
        path = backup.getQueryRouterPaths().get(queryRouterPathIndex);
      } else {
        path = null;
      }
    } else {
      if (backup.getPaths() != null && backup.getPaths().size() > index) {
        path = backup.getPaths().get(index);
      } else {
        path = null;
      }
    }
    return path;
  }

  void setConfigurationsCredentials(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec) {
    if (spec.getConfigurations() == null) {
      spec.setConfigurations(new StackGresClusterConfigurations());
    }
    spec.getConfigurations().setCredentials(new StackGresClusterCredentials());
    spec.getConfigurations().getCredentials()
        .setPatroni(new StackGresClusterPatroniCredentials());
    spec.getConfigurations().getCredentials().getPatroni()
        .setRestApiPassword(new SecretKeySelector(
            StackGresPasswordKeys.RESTAPI_PASSWORD_KEY,
            cluster.getMetadata().getName()));
    spec.getConfigurations().getCredentials()
        .setUsers(new StackGresClusterUsersCredentials());
    spec.getConfigurations().getCredentials().getUsers()
        .setSuperuser(new StackGresClusterUserSecretKeyRef());
    spec.getConfigurations().getCredentials().getUsers().getSuperuser()
        .setUsername(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_USERNAME_KEY,
            cluster.getMetadata().getName()));
    spec.getConfigurations().getCredentials().getUsers().getSuperuser()
        .setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY,
            cluster.getMetadata().getName()));
    spec.getConfigurations().getCredentials().getUsers()
        .setReplication(new StackGresClusterUserSecretKeyRef());
    spec.getConfigurations().getCredentials().getUsers().getReplication()
        .setUsername(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_USERNAME_KEY,
            cluster.getMetadata().getName()));
    spec.getConfigurations().getCredentials().getUsers().getReplication()
        .setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_KEY,
            cluster.getMetadata().getName()));
    spec.getConfigurations().getCredentials().getUsers()
        .setAuthenticator(new StackGresClusterUserSecretKeyRef());
    spec.getConfigurations().getCredentials().getUsers().getAuthenticator()
        .setUsername(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_USERNAME_KEY,
            cluster.getMetadata().getName()));
    spec.getConfigurations().getCredentials().getUsers().getAuthenticator()
        .setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_KEY,
            cluster.getMetadata().getName()));
  }

  void setMetadata(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    if (spec.getMetadata() == null) {
      spec.setMetadata(new StackGresClusterSpecMetadata());
    }
    setLabels(cluster, spec, index);
    setAnnotations(cluster, spec, index);
  }

  void setLabels(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    if (spec.getMetadata().getLabels() == null
        && cluster.getSpec().getMetadata() != null
        && cluster.getSpec().getMetadata().getLabels() != null) {
      spec.getMetadata().setLabels(cluster.getSpec().getMetadata().getLabels());
    }
  }

  void setAnnotations(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    if (spec.getMetadata().getAnnotations() == null
        && cluster.getSpec().getMetadata() != null
        && cluster.getSpec().getMetadata().getAnnotations() != null) {
      spec.getMetadata().setAnnotations(cluster.getSpec().getMetadata().getAnnotations());
    }
  }

  Map<String, String> mergeMaps(
      Map<String, String> spec,
      Map.Entry<String, String> extraEntry) {
    return Seq.seq(Optional.ofNullable(spec))
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .filter(e -> !Objects.equals(extraEntry.getKey(), e.getKey()))
        .append(extraEntry)
        .toMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  void setInitialData(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    var initialData = cluster.getSpec().getInitialData();
    if (initialData != null
        && initialData.getRestore() != null
        && initialData.getRestore().getFromBackup() != null
        && initialData.getRestore().getFromBackup().getName() != null
        && cluster.getStatus() != null
        && cluster.getStatus().getSgBackups() != null
        && !cluster.getStatus().getSgBackups().isEmpty()) {
      int plainIndex = StackGresShardedClusterUtil.getPlainIndex(cluster, index);
      if (plainIndex >= cluster.getStatus().getSgBackups().size()) {
        if (plainIndex == 0) {
          throw new RuntimeException("SGShardedBackup " + initialData.getRestore().getFromBackup().getName()
              + " has no backup for coordinator at index " + plainIndex);
        }
        if (StackGresShardedClusterUtil.isQueryRouterIndex(cluster, index)) {
          throw new RuntimeException("SGShardedBackup " + initialData.getRestore().getFromBackup().getName()
              + " has no backup for query router at index " + plainIndex);
        }
        throw new RuntimeException("SGShardedBackup " + initialData.getRestore().getFromBackup().getName()
            + " has no backup for worker at index " + plainIndex);
      }
      var fromBackup = initialData.getRestore().getFromBackup();
      var restoreFromBackupBuilder = new StackGresClusterRestoreFromBackupBuilder()
          .withName(cluster.getStatus().getSgBackups().get(plainIndex));
      if (initialData.getRestore().getFromBackup().getPointInTimeRecovery() != null) {
        restoreFromBackupBuilder
            .withNewPointInTimeRecovery()
            .withRestoreToTimestamp(
                initialData.getRestore().getFromBackup().getPointInTimeRecovery().getRestoreToTimestamp())
            .endPointInTimeRecovery();
      } else {
        restoreFromBackupBuilder
            .withTargetName(fromBackup.getName());
      }
      spec.setInitialData(new StackGresClusterInitialDataBuilder()
          .withNewRestore()
          .withFromBackup(restoreFromBackupBuilder.build())
          .endRestore()
          .build());
      if (fromBackup.getPointInTimeRecovery() != null) {
        spec.getInitialData().getRestore().getFromBackup().setPointInTimeRecovery(
            new StackGresClusterRestorePitrBuilder()
            .withRestoreToTimestamp(fromBackup.getPointInTimeRecovery().getRestoreToTimestamp())
            .build());
      }
    }
  }

  void setManagedSql(
      StackGresShardedCluster cluster,
      StackGresClusterSpec spec,
      int index) {
    final String defaultScript = ManagedSqlUtil.defaultName(
        StackGresShardedClusterUtil.getClusterName(cluster, index));
    spec.setManagedSql(new StackGresClusterManagedSqlBuilder(spec.getManagedSql())
        .withScripts(
            Seq.of(new StackGresClusterManagedScriptEntryBuilder()
                .withId(0)
                .withSgScript(defaultScript)
                .build())
            .append(
                Optional.ofNullable(spec.getManagedSql())
                .map(StackGresClusterManagedSql::getScripts)
                .orElse(List.of()))
            .toList())
        .build());
  }

  void setClusterSpecFromShardOverrides(
      StackGresShardedCluster cluster,
      StackGresShardedClusterWorker specOverride,
      StackGresClusterSpec spec,
      int globalIndex) {
    if (specOverride.getConfigurationsForWorkers() != null) {
      if (specOverride.getConfigurationsForWorkers().getSgPostgresConfig() != null) {
        spec.getConfigurations().setSgPostgresConfig(
            specOverride.getConfigurationsForWorkers().getSgPostgresConfig());
      }
      if (specOverride.getConfigurationsForWorkers().getSgPoolingConfig() != null) {
        spec.getConfigurations().setSgPoolingConfig(
            specOverride.getConfigurationsForWorkers().getSgPoolingConfig());
      }
      if (specOverride.getConfigurationsForWorkers().getPostgres() != null
          && specOverride.getConfigurationsForWorkers().getPostgres().getWalPath() != null) {
        if (spec.getConfigurations().getPostgres() == null) {
          spec.getConfigurations().setPostgres(new StackGresClusterConfigurationsPostgres());
        }
        spec.getConfigurations().getPostgres().setWalPath(
            specOverride.getConfigurationsForWorkers().getPostgres().getWalPath());
      }
      if (specOverride.getConfigurationsForWorkers().getPostgresExporter() != null) {
        var queriesFound = Optional.ofNullable(spec.getConfigurations())
            .map(StackGresClusterConfigurations::getPostgresExporter)
            .map(StackGresClusterPostgresExporter::getQueries);
        spec.getConfigurations().setPostgresExporter(
            specOverride.getConfigurationsForWorkers().getPostgresExporter());
        spec.getConfigurations().getPostgresExporter().setQueries(
            Optional.of(spec.getConfigurations().getPostgresExporter())
            .map(StackGresClusterPostgresExporter::getQueries)
            .map(queries -> queriesFound
                .map(q -> mergeMap(q, queries))
                .orElse(queries))
            .or(() -> queriesFound)
            .map(StackGresClusterPostgresExporterQueries::new)
            .orElse(null));
      }
    }
    if (specOverride.getInstancesPerCluster() != null) {
      spec.setInstances(specOverride.getInstancesPerCluster());
    }
    if (specOverride.getSgInstanceProfile() != null) {
      spec.setSgInstanceProfile(specOverride.getSgInstanceProfile());
    }
    if (specOverride.getReplicationForWorkers() != null) {
      spec.setReplication(specOverride.getReplicationForWorkers());
    }
    if (specOverride.getManagedSql() != null) {
      final String defaultScript = ManagedSqlUtil.defaultName(
          StackGresShardedClusterUtil.getClusterName(cluster, globalIndex));
      spec.setManagedSql(new StackGresClusterManagedSqlBuilder(specOverride.getManagedSql())
          .withScripts(
              Seq.of(new StackGresClusterManagedScriptEntryBuilder()
                  .withId(0)
                  .withSgScript(defaultScript)
                  .build())
              .append(
                  Optional.ofNullable(specOverride.getManagedSql())
                  .map(StackGresClusterManagedSql::getScripts)
                  .orElse(List.of()))
              .toList())
          .build());
    }
    if (specOverride.getMetadata() != null) {
      if (spec.getMetadata() == null) {
        spec.setMetadata(new StackGresClusterSpecMetadata());
      }
      setOverridesLabels(specOverride, spec, globalIndex);
      setOverridesAnnotations(specOverride, spec, globalIndex);
    }
    if (specOverride.getPodsForWorkers() != null) {
      if (specOverride.getPodsForWorkers().getLivenessProbe() != null) {
        spec.getPods().setLivenessProbe(
            specOverride.getPodsForWorkers().getLivenessProbe());
      }
      if (specOverride.getPodsForWorkers().getReadinessProbe() != null) {
        spec.getPods().setReadinessProbe(
            specOverride.getPodsForWorkers().getReadinessProbe());
      }
      if (specOverride.getPodsForWorkers().getTerminationGracePeriodSeconds() != null) {
        spec.getPods().setTerminationGracePeriodSeconds(
            specOverride.getPodsForWorkers().getTerminationGracePeriodSeconds());
      }
      if (specOverride.getPodsForWorkers().getDisableConnectionPooling() != null) {
        spec.getPods().setDisableConnectionPooling(
            specOverride.getPodsForWorkers().getDisableConnectionPooling());
      }
      if (specOverride.getPodsForWorkers().getDisableMetricsExporter() != null) {
        spec.getPods().setDisableMetricsExporter(
            specOverride.getPodsForWorkers().getDisableMetricsExporter());
      }
      if (specOverride.getPodsForWorkers().getDisablePostgresUtil() != null) {
        spec.getPods().setDisablePostgresUtil(
            specOverride.getPodsForWorkers().getDisablePostgresUtil());
      }
      if (specOverride.getPodsForWorkers().getManagementPolicy() != null) {
        spec.getPods().setManagementPolicy(specOverride.getPodsForWorkers().getManagementPolicy());
      }
      if (specOverride.getPodsForWorkers().getUpdateStrategy() != null) {
        spec.getPods().setUpdateStrategy(specOverride.getPodsForWorkers().getUpdateStrategy());
      }
      if (specOverride.getPodsForWorkers().getPersistentVolume() != null) {
        if (specOverride.getPodsForWorkers().getPersistentVolume().getSize() != null) {
          spec.getPods().getPersistentVolume().setSize(
              specOverride.getPodsForWorkers().getPersistentVolume().getSize());
        }
        if (specOverride.getPodsForWorkers().getPersistentVolume().getStorageClass() != null) {
          spec.getPods().getPersistentVolume().setStorageClass(
              specOverride.getPodsForWorkers().getPersistentVolume().getStorageClass());
        }
        if (specOverride.getPodsForWorkers().getPersistentVolume().getVolumeAttributesClassName() != null) {
          spec.getPods().getPersistentVolume().setVolumeAttributesClassName(
              specOverride.getPodsForWorkers().getPersistentVolume().getVolumeAttributesClassName());
        }
        if (specOverride.getPodsForWorkers().getPersistentVolume().getFsGroupChangePolicy() != null) {
          spec.getPods().getPersistentVolume().setFsGroupChangePolicy(
              specOverride.getPodsForWorkers().getPersistentVolume().getFsGroupChangePolicy());
        }
        if (specOverride.getPodsForWorkers().getPersistentVolume().getIoLimits() != null) {
          spec.getPods().getPersistentVolume().setIoLimits(
              specOverride.getPodsForWorkers().getPersistentVolume().getIoLimits());
        }
      }
      if (specOverride.getPodsForWorkers().getResources() != null) {
        if (spec.getPods().getResources() == null) {
          spec.getPods().setResources(new StackGresClusterResources());
        }
        if (specOverride.getPodsForWorkers().getResources()
            .getContainers() != null) {
          spec.getPods().getResources().setContainers(
              specOverride.getPodsForWorkers().getResources().getContainers());
        }
        if (specOverride.getPodsForWorkers().getResources()
            .getInitContainers() != null) {
          spec.getPods().getResources().setInitContainers(
              specOverride.getPodsForWorkers().getResources().getInitContainers());
        }
        if (specOverride.getPodsForWorkers().getResources()
            .getEnableClusterLimitsRequirements() != null) {
          spec.getPods().getResources().setEnableClusterLimitsRequirements(
              specOverride.getPodsForWorkers().getResources().getEnableClusterLimitsRequirements());
        }
        if (specOverride.getPodsForWorkers().getResources()
            .getDisableResourcesRequestsSplitFromTotal() != null) {
          spec.getPods().getResources().setDisableResourcesRequestsSplitFromTotal(
              specOverride.getPodsForWorkers().getResources()
              .getDisableResourcesRequestsSplitFromTotal());
        }
        if (specOverride.getPodsForWorkers().getResources()
            .getFailWhenTotalIsHigher() != null) {
          spec.getPods().getResources().setFailWhenTotalIsHigher(
              specOverride.getPodsForWorkers().getResources()
              .getFailWhenTotalIsHigher());
        }
      }
      if (specOverride.getPodsForWorkers().getScheduling() != null) {
        spec.getPods().setScheduling(
            specOverride.getPodsForWorkers().getScheduling());
      }
      if (specOverride.getPodsForWorkers().getCustomVolumes() != null) {
        spec.getPods().setCustomVolumes(specOverride.getPodsForWorkers().getCustomVolumes());
      }
      if (specOverride.getPodsForWorkers().getCustomPersistentVolumes() != null) {
        spec.getPods().setCustomPersistentVolumes(
            specOverride.getPodsForWorkers().getCustomPersistentVolumes());
      }
      if (specOverride.getPodsForWorkers().getCustomContainers() != null) {
        spec.getPods().setCustomContainers(specOverride.getPodsForWorkers().getCustomContainers());
      }
      if (specOverride.getPodsForWorkers().getCustomInitContainers() != null) {
        spec.getPods().setCustomInitContainers(
            specOverride.getPodsForWorkers().getCustomInitContainers());
      }
      if (specOverride.getPodsForWorkers().getCustomVolumeMounts() != null) {
        spec.getPods().setCustomVolumeMounts(specOverride.getPodsForWorkers().getCustomVolumeMounts());
      }
      if (specOverride.getPodsForWorkers().getCustomInitVolumeMounts() != null) {
        spec.getPods().setCustomInitVolumeMounts(
            specOverride.getPodsForWorkers().getCustomInitVolumeMounts());
      }
      if (specOverride.getPodsForWorkers().getCustomEnv() != null) {
        spec.getPods().setCustomEnv(
            specOverride.getPodsForWorkers().getCustomEnv());
      }
      if (specOverride.getPodsForWorkers().getCustomInitEnv() != null) {
        spec.getPods().setCustomInitEnv(
            specOverride.getPodsForWorkers().getCustomInitEnv());
      }
      if (specOverride.getPodsForWorkers().getCustomEnvFrom() != null) {
        spec.getPods().setCustomEnvFrom(
            specOverride.getPodsForWorkers().getCustomEnvFrom());
      }
      if (specOverride.getPodsForWorkers().getCustomInitEnvFrom() != null) {
        spec.getPods().setCustomInitEnvFrom(
            specOverride.getPodsForWorkers().getCustomInitEnvFrom());
      }
    }
  }

  void setOverridesLabels(StackGresShardedClusterWorker specOverride, StackGresClusterSpec spec, int index) {
    if (specOverride.getMetadata().getLabels() != null) {
      spec.getMetadata().setLabels(specOverride.getMetadata().getLabels());
    }
  }

  void setOverridesAnnotations(StackGresShardedClusterWorker specOverride, StackGresClusterSpec spec,
      int index) {
    if (specOverride.getMetadata().getAnnotations() != null) {
      spec.getMetadata().setAnnotations(specOverride.getMetadata().getAnnotations());
    }
  }

  protected final <K, V> Map<K, V> mergeMap(Map<K, V> initial, Map<K, V> override) {
    var map = new HashMap<K, V>(initial);
    map.putAll(override);
    return map;
  }
}
