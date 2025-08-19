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
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresCoordinatorServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresShardsServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShard;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import org.jooq.lambda.Seq;

public abstract class StackGresShardedClusterForUtil implements StackGresShardedClusterUtil {

  StackGresCluster getCoordinatorCluster(StackGresShardedCluster cluster) {
    final StackGresClusterSpec spec =
        new StackGresClusterSpecBuilder(cluster.getSpec().getCoordinator())
        .withConfigurations(cluster.getSpec().getCoordinator().getConfigurationsForCoordinator())
        .build();
    setClusterSpecFromShardedCluster(cluster, spec, 0);
    if (cluster.getSpec().getCoordinator().getReplicationForCoordinator() != null) {
      spec.setReplication(cluster.getSpec().getCoordinator().getReplicationForCoordinator());
    } else {
      spec.setReplication(cluster.getSpec().getReplication());
    }
    updateCoordinatorSpec(cluster, spec);
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

  StackGresCluster getShardsCluster(StackGresShardedCluster cluster, int index) {
    final StackGresClusterSpec spec =
        new StackGresClusterSpecBuilder(cluster.getSpec().getShards())
        .build();
    setClusterSpecFromShardedCluster(cluster, spec, index + 1);
    if (cluster.getSpec().getShards().getReplicationForShards() != null) {
      spec.setReplication(cluster.getSpec().getShards().getReplicationForShards());
    } else {
      spec.setReplication(cluster.getSpec().getReplication());
    }
    spec.setInstances(cluster.getSpec().getShards().getInstancesPerCluster());
    Optional.of(cluster.getSpec().getShards())
        .map(StackGresShardedClusterShards::getOverrides)
        .stream()
        .flatMap(List::stream)
        .filter(specOverride -> Objects.equals(
            specOverride.getIndex(),
            index))
        .findFirst()
        .ifPresent(specOverride -> setClusterSpecFromShardOverrides(
            cluster, specOverride, spec, index + 1));
    updateShardsClusterSpec(cluster, spec, index);
    StackGresCluster shardsCluster = new StackGresCluster();
    shardsCluster.setMetadata(new ObjectMeta());
    shardsCluster.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    shardsCluster.getMetadata().setName(
        StackGresShardedClusterUtil.getShardClusterName(cluster, index));
    var postgresServices = cluster.getSpec().getPostgresServices();
    spec.setPostgresServices(new StackGresPostgresServicesBuilder()
        .withNewPrimary()
        .withEnabled(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getShards)
            .map(StackGresShardedClusterPostgresShardsServices::getPrimaries)
            .map(StackGresPostgresService::getEnabled)
            .orElse(true))
        .withCustomPorts(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getShards)
            .map(StackGresShardedClusterPostgresShardsServices::getCustomPorts)
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
    shardsCluster.setSpec(spec);
    return shardsCluster;
  }

  abstract void updateShardsClusterSpec(
      StackGresShardedCluster cluster,
      StackGresClusterSpec spec,
      int index);

  void setClusterSpecFromShardedCluster(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    spec.setProfile(cluster.getSpec().getProfile());
    setPostgres(cluster, spec);
    setPostgresSsl(cluster, spec);
    setPostgresExtensions(cluster, spec);
    setConfigurationsObservability(cluster, spec);
    setConfigurationsPostgresExporter(cluster, spec);
    setConfigurationsBackups(cluster, spec, index);
    setConfigurationsCredentials(cluster, spec);
    setMetadata(cluster, spec, index);
    setInitialData(cluster, spec, index);
    setManagedSql(cluster, spec, index);
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
        .map(StackGresShardedClusterStatus::getToInstallPostgresExtensions)
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
          spec.getConfigurations().setPostgresExporter(postgresExporter);
          spec.getConfigurations().getPostgresExporter().setQueries(
              Optional.of(spec.getConfigurations().getPostgresExporter())
              .map(StackGresClusterPostgresExporter::getQueries)
              .map(queries -> queriesFound
                  .map(q -> mergeMap(queries, q))
                  .orElse(queries))
              .or(() -> queriesFound)
              .map(StackGresClusterPostgresExporterQueries::new)
              .orElse(null));
          spec.getConfigurations().setPostgresExporter(postgresExporter);
        });
  }

  void setConfigurationsBackups(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    Optional.ofNullable(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getBackups)
        .filter(Predicate.not(List::isEmpty))
        .map(backups -> backups.getFirst())
        .filter(backup -> backup.getPaths() != null)
        .ifPresent(backup -> {
          if (spec.getConfigurations() == null) {
            spec.setConfigurations(new StackGresClusterConfigurations());
          }
          spec.getConfigurations().setBackups(List.of(
              new StackGresClusterBackupConfigurationBuilder()
              .withSgObjectStorage(backup.getSgObjectStorage())
              .withPath(backup.getPaths().get(index))
              .withRetention(backup.getRetention())
              .withCompression(backup.getCompression())
              .withPerformance(backup.getPerformance())
              .withUseVolumeSnapshot(backup.getUseVolumeSnapshot())
              .withVolumeSnapshotClass(backup.getVolumeSnapshotClass())
              .withFastVolumeSnapshot(backup.getFastVolumeSnapshot())
              .withTimeout(backup.getTimeout())
              .withReconciliationTimeout(backup.getReconciliationTimeout())
              .withMaxRetries(backup.getMaxRetries())
              .withRetainWalsForUnmanagedLifecycle(backup.getRetainWalsForUnmanagedLifecycle())
              .build()));
        });
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
        && !cluster.getStatus().getSgBackups().isEmpty()
        && index < cluster.getStatus().getSgBackups().size()) {
      var fromBackup = initialData
          .getRestore().getFromBackup();
      var restoreFromBackupBuilder = new StackGresClusterRestoreFromBackupBuilder()
          .withName(cluster.getStatus().getSgBackups().get(index));
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
      StackGresShardedClusterShard specOverride,
      StackGresClusterSpec spec,
      int index) {
    if (specOverride.getConfigurationsForShards() != null) {
      if (specOverride.getConfigurationsForShards().getSgPostgresConfig() != null) {
        spec.getConfigurations().setSgPostgresConfig(
            specOverride.getConfigurationsForShards().getSgPostgresConfig());
      }
      if (specOverride.getConfigurationsForShards().getSgPoolingConfig() != null) {
        spec.getConfigurations().setSgPoolingConfig(
            specOverride.getConfigurationsForShards().getSgPoolingConfig());
      }
      if (specOverride.getConfigurationsForShards().getPostgresExporter() != null) {
        var queriesFound = Optional.ofNullable(spec.getConfigurations())
            .map(StackGresClusterConfigurations::getPostgresExporter)
            .map(StackGresClusterPostgresExporter::getQueries);
        spec.getConfigurations().setPostgresExporter(
            specOverride.getConfigurationsForShards().getPostgresExporter());
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
    if (specOverride.getReplicationForShards() != null) {
      spec.setReplication(specOverride.getReplicationForShards());
    }
    if (specOverride.getManagedSql() != null) {
      final String defaultScript = ManagedSqlUtil.defaultName(
          StackGresShardedClusterUtil.getClusterName(cluster, index));
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
      setOverridesLabels(specOverride, spec, index);
      setOverridesAnnotations(specOverride, spec, index);
    }
    if (specOverride.getPodsForShards() != null) {
      if (specOverride.getPodsForShards().getDisableConnectionPooling() != null) {
        spec.getPods().setDisableConnectionPooling(
            specOverride.getPodsForShards().getDisableConnectionPooling());
      }
      if (specOverride.getPodsForShards().getDisableMetricsExporter() != null) {
        spec.getPods().setDisableMetricsExporter(
            specOverride.getPodsForShards().getDisableMetricsExporter());
      }
      if (specOverride.getPodsForShards().getDisablePostgresUtil() != null) {
        spec.getPods().setDisablePostgresUtil(
            specOverride.getPodsForShards().getDisablePostgresUtil());
      }
      if (specOverride.getPodsForShards().getManagementPolicy() != null) {
        spec.getPods().setManagementPolicy(specOverride.getPodsForShards().getManagementPolicy());
      }
      if (specOverride.getPodsForShards().getPersistentVolume() != null) {
        if (specOverride.getPodsForShards().getPersistentVolume().getSize() != null) {
          spec.getPods().getPersistentVolume().setSize(
              specOverride.getPodsForShards().getPersistentVolume().getSize());
        }
        if (specOverride.getPodsForShards().getPersistentVolume().getStorageClass() != null) {
          spec.getPods().getPersistentVolume().setStorageClass(
              specOverride.getPodsForShards().getPersistentVolume().getStorageClass());
        }
      }
      if (specOverride.getPodsForShards().getResources() != null) {
        if (spec.getPods().getResources() == null) {
          spec.getPods().setResources(new StackGresClusterResources());
        }
        if (specOverride.getPodsForShards().getResources()
            .getContainers() != null) {
          spec.getPods().getResources().setContainers(
              specOverride.getPodsForShards().getResources().getContainers());
        }
        if (specOverride.getPodsForShards().getResources()
            .getInitContainers() != null) {
          spec.getPods().getResources().setInitContainers(
              specOverride.getPodsForShards().getResources().getInitContainers());
        }
        if (specOverride.getPodsForShards().getResources()
            .getEnableClusterLimitsRequirements() != null) {
          spec.getPods().getResources().setEnableClusterLimitsRequirements(
              specOverride.getPodsForShards().getResources().getEnableClusterLimitsRequirements());
        }
        if (specOverride.getPodsForShards().getResources()
            .getDisableResourcesRequestsSplitFromTotal() != null) {
          spec.getPods().getResources().setDisableResourcesRequestsSplitFromTotal(
              specOverride.getPodsForShards().getResources()
              .getDisableResourcesRequestsSplitFromTotal());
        }
        if (specOverride.getPodsForShards().getResources()
            .getFailWhenTotalIsHigher() != null) {
          spec.getPods().getResources().setFailWhenTotalIsHigher(
              specOverride.getPodsForShards().getResources()
              .getFailWhenTotalIsHigher());
        }
      }
      if (specOverride.getPodsForShards().getScheduling() != null) {
        spec.getPods().setScheduling(
            specOverride.getPodsForShards().getScheduling());
      }
      if (specOverride.getPodsForShards().getCustomVolumes() != null) {
        spec.getPods().setCustomVolumes(specOverride.getPodsForShards().getCustomVolumes());
      }
      if (specOverride.getPodsForShards().getCustomContainers() != null) {
        spec.getPods().setCustomContainers(specOverride.getPodsForShards().getCustomContainers());
      }
      if (specOverride.getPodsForShards().getCustomInitContainers() != null) {
        spec.getPods().setCustomInitContainers(
            specOverride.getPodsForShards().getCustomInitContainers());
      }
      if (specOverride.getPodsForShards().getCustomVolumeMounts() != null) {
        spec.getPods().setCustomVolumeMounts(specOverride.getPodsForShards().getCustomVolumeMounts());
      }
      if (specOverride.getPodsForShards().getCustomInitVolumeMounts() != null) {
        spec.getPods().setCustomInitVolumeMounts(
            specOverride.getPodsForShards().getCustomInitVolumeMounts());
      }
    }
  }

  void setOverridesLabels(StackGresShardedClusterShard specOverride, StackGresClusterSpec spec, int index) {
    if (specOverride.getMetadata().getLabels() != null) {
      spec.getMetadata().setLabels(specOverride.getMetadata().getLabels());
    }
  }

  void setOverridesAnnotations(StackGresShardedClusterShard specOverride, StackGresClusterSpec spec,
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
