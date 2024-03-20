/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.external.shardingsphere.ComputeNode;
import io.stackgres.common.crd.external.shardingsphere.ComputeNodeBuilder;
import io.stackgres.common.crd.external.shardingsphere.ComputeNodePortBindingBuilder;
import io.stackgres.common.crd.external.shardingsphere.ComputeNodeUserBuilder;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
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
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServicesBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestorePitrBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.crd.sgconfig.StackGresConfigShardingSphere;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptBuilder;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresCoordinatorServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresShardsServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShard;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphere;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereAuthority;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereEtcd;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSpherePrivilege;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereRepository;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereZooKeeper;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingSphereRepositoryType;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.factory.cluster.ClusterDefaultScripts;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public interface StackGresShardedClusterForShardingSphereUtil extends StackGresShardedClusterUtil {

  static StackGresCluster getCoordinatorCluster(StackGresShardedCluster cluster) {
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
    StackGresCluster coordinatorCluster = new StackGresCluster();
    coordinatorCluster.setMetadata(new ObjectMeta());
    coordinatorCluster.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    coordinatorCluster.getMetadata().setName(
        StackGresShardedClusterUtil.getCoordinatorClusterName(cluster));
    var postgresServices = cluster.getSpec().getPostgresServices();
    spec.setPostgresServices(new StackGresClusterPostgresServicesBuilder()
        .withNewPrimary()
        .withEnabled(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getCoordinator)
            .map(StackGresShardedClusterPostgresCoordinatorServices::getPrimary)
            .map(StackGresPostgresService::getEnabled)
            .orElse(true))
        .withCustomPorts(
            Optional.ofNullable(postgresServices)
            .map(StackGresShardedClusterPostgresServices::getCoordinator)
            .map(StackGresShardedClusterPostgresCoordinatorServices::getCustomPorts)
            .orElse(null))
        .endPrimary()
        .withNewReplicas()
        .withEnabled(false)
        .endReplicas()
        .build());
    coordinatorCluster.setSpec(spec);
    return coordinatorCluster;
  }

  static StackGresCluster getShardsCluster(StackGresShardedCluster cluster, int index) {
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
    if (spec.getManagedSql() == null) {
      spec.setManagedSql(new StackGresClusterManagedSql());
    }
    spec.getManagedSql().setScripts(
        Seq.seq(Optional.ofNullable(spec.getManagedSql().getScripts())
            .stream()
            .flatMap(List::stream)
            .limit(1))
        .append(Seq.of(new StackGresClusterManagedScriptEntryBuilder()
            .withSgScript(StackGresShardedClusterUtil.coordinatorScriptName(cluster))
            .withId(1)
            .build())
            .filter(ignore -> index == 0))
        .append(new StackGresClusterManagedScriptEntryBuilder()
            .withSgScript(StackGresShardedClusterUtil.shardsScriptName(cluster))
            .withId(2)
            .build())
        .append(Optional.ofNullable(spec.getManagedSql().getScripts())
            .stream()
            .flatMap(List::stream)
            .skip(1))
        .append(Optional.ofNullable(cluster.getSpec().getCoordinator().getManagedSql())
            .filter(ignore -> index == 0)
            .map(StackGresClusterManagedSql::getScripts)
            .stream()
            .flatMap(List::stream)
            .map(script -> new StackGresClusterManagedScriptEntryBuilder(script)
              .withId(script.getId() + 1000000)
              .build()))
        .toList());
    StackGresCluster shardsCluster = new StackGresCluster();
    shardsCluster.setMetadata(new ObjectMeta());
    shardsCluster.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    shardsCluster.getMetadata().setName(
        StackGresShardedClusterUtil.getShardClusterName(cluster, index));
    var postgresServices = cluster.getSpec().getPostgresServices();
    spec.setPostgresServices(new StackGresClusterPostgresServicesBuilder()
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
            .orElse(null))
        .endPrimary()
        .withNewReplicas()
        .withEnabled(false)
        .endReplicas()
        .build());
    shardsCluster.setSpec(spec);
    return shardsCluster;
  }

  private static void setClusterSpecFromShardedCluster(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    spec.setProfile(cluster.getSpec().getProfile());
    setPostgres(cluster, spec);
    setPostgresSsl(cluster, spec);
    setPostgresExtensions(cluster, spec);
    setConfigurationsBackups(cluster, spec, index);
    setConfigurationsCredentials(cluster, spec);
    setMetadata(cluster, spec, index);
    setInitialData(cluster, spec, index);
    setManagedSql(cluster, spec, index);
    spec.setDistributedLogs(cluster.getSpec().getDistributedLogs());
    spec.setPrometheusAutobind(cluster.getSpec().getPrometheusAutobind());
    spec.setNonProductionOptions(cluster.getSpec().getNonProductionOptions());
  }

  private static void setPostgres(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec) {
    spec.setPostgres(
        new StackGresClusterPostgresBuilder(cluster.getSpec().getPostgres())
        .build());
  }

  private static void setPostgresSsl(
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

  private static void setPostgresExtensions(
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
    if (cluster.getStatus() != null) {
      spec.setToInstallPostgresExtensions(cluster.getStatus().getToInstallPostgresExtensions());
    }
  }

  private static void setConfigurationsBackups(
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
              .build()));
        });
  }

  private static void setConfigurationsCredentials(
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

  private static void setMetadata(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    if (spec.getMetadata() == null) {
      spec.setMetadata(new StackGresClusterSpecMetadata());
    }
    if (spec.getMetadata().getLabels() == null
        && cluster.getSpec().getMetadata() != null
        && cluster.getSpec().getMetadata().getLabels() != null) {
      spec.getMetadata().setLabels(cluster.getSpec().getMetadata().getLabels());
    }
    if (spec.getMetadata().getAnnotations() == null
        && cluster.getSpec().getMetadata() != null
        && cluster.getSpec().getMetadata().getAnnotations() != null) {
      spec.getMetadata().setAnnotations(cluster.getSpec().getMetadata().getAnnotations());
    }
  }

  private static void setInitialData(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    var initialData = cluster.getSpec().getInitialData();
    if (initialData != null
        && initialData.getRestore() != null
        && initialData.getRestore().getFromBackup() != null
        && initialData.getRestore().getFromBackup().getName() != null
        && cluster.getStatus() != null
        && cluster.getStatus().getSgBackups() != null
        && !cluster.getStatus().getSgBackups().isEmpty()) {
      var fromBackup = initialData
          .getRestore().getFromBackup();
      spec.setInitialData(new StackGresClusterInitialDataBuilder()
          .withNewRestore()
          .withNewFromBackup()
          .withName(cluster.getStatus().getSgBackups().get(index))
          .withTargetName(fromBackup.getName())
          .endFromBackup()
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

  private static void setManagedSql(
      StackGresShardedCluster cluster,
      StackGresClusterSpec spec,
      int index) {
    final String defaultScript = ManagedSqlUtil.defaultName(
        StackGresShardedClusterUtil.getClusterName(cluster, index));
    spec.setManagedSql(new StackGresClusterManagedSqlBuilder(spec.getManagedSql())
        .withScripts(
            Seq
            .of(new StackGresClusterManagedScriptEntryBuilder()
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

  private static void setClusterSpecFromShardOverrides(
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
              Seq
              .of(new StackGresClusterManagedScriptEntryBuilder()
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
      if (specOverride.getMetadata().getLabels() != null) {
        spec.getMetadata().setLabels(specOverride.getMetadata().getLabels());
      }
      if (specOverride.getMetadata().getAnnotations() != null) {
        spec.getMetadata().setAnnotations(specOverride.getMetadata().getAnnotations());
      }
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
    }
  }

  static ComputeNode getCoordinatorComputeNode(
      StackGresShardedClusterContext context,
      Map<String, String> coordinatorLabelsWithoutUid) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    var superuserCredentials = ShardedClusterSecret.getSuperuserCredentials(context);
    var shardingSphere = cluster.getSpec().getCoordinator()
        .getConfigurationsForCoordinator().getShardingSphere();
    return
        new ComputeNodeBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.coordinatorScriptName(cluster))
            .build())
        .editSpec()
        .withServerVersion("5.4.1")
        .withNewStorageNodeConnector()
        .withType("postgresql")
        .withVersion("42.4.3")
        .endStorageNodeConnector()
        .withSelector(new LabelSelectorBuilder()
            .withMatchLabels(coordinatorLabelsWithoutUid)
            .build())
        .withReplicas(cluster.getSpec().getCoordinator().getInstances())
        .withPortBindings(new ComputeNodePortBindingBuilder()
            .withName(EnvoyUtil.POSTGRES_PORT_NAME)
            .withContainerPort(5432)
            .withServicePort(PatroniUtil.POSTGRES_SERVICE_PORT)
            .withProtocol("TCP")
            .build())
        .withServiceType(cluster.getSpec().getPostgresServices().getCoordinator()
            .getPrimary().getType())
        .withNewBootstrap()
        .withNewServerConfig()
        .withProps(Seq.of(Map.ofEntries(
            Map.entry("proxy-frontend-database-protocol-type", "PostgreSQL"),
            Map.entry("proxy-default-port", "5432")))
            .flatMap(map -> Seq.seq(map)
                .append(Optional.of(shardingSphere)
                    .map(StackGresShardedClusterShardingSphere::getProperties)
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .filter(e -> !map.containsKey(e.getKey()))
                    .map(e -> Tuple.tuple(e.getKey(), e.getValue()))))
            .toMap(Tuple2::v1, Tuple2::v2))
        .withNewMode()
        .withType(shardingSphere.getMode().getType())
        .withNewRepository()
        .withType(shardingSphere.getMode().getRepository().getType())
        .withProps(getCoordinatorRepositoryProps(shardingSphere.getMode().getRepository()))
        .endRepository()
        .endMode()
        .withNewAuthority()
        .withNewPrivilege()
        .withType(Optional.of(shardingSphere)
            .map(StackGresShardedClusterShardingSphere::getAuthority)
            .map(StackGresShardedClusterShardingSphereAuthority::getPrivilege)
            .map(StackGresShardedClusterShardingSpherePrivilege::getType)
            .orElse(null))
        .endPrivilege()
        .withUsers(Seq.of(new ComputeNodeUserBuilder()
            .withUser(superuserCredentials.v1)
            .withPassword(superuserCredentials.v2)
            .build())
            .append(context.getShardingSphereAuthorityUsers()
                .stream()
                .map(user -> new ComputeNodeUserBuilder()
                    .withUser(user.v1)
                    .withPassword(user.v2)
                    .build()))
            .toList())
        .endAuthority()
        .endServerConfig()
        .endBootstrap()
        .endSpec()
        .build();
  }

  private static Map<String, String> getCoordinatorRepositoryProps(
      StackGresShardedClusterShardingSphereRepository repository) {
    switch (StackGresShardingSphereRepositoryType.fromString(repository.getType())) {
      case ETCD:
        var etcd = Optional.of(repository)
            .map(StackGresShardedClusterShardingSphereRepository::getEtcd);
        return Seq.of(Seq.<Tuple2<String, String>>of()
            .append(etcd
                .map(StackGresShardedClusterShardingSphereEtcd::getServerList)
                .map(value -> Tuple.tuple("server-lists", Seq.seq(value).toString(","))))
            .toMap(Tuple2::v1, Tuple2::v2))
            .flatMap(map -> Seq.seq(map)
                .append(Optional.of(repository)
                    .map(StackGresShardedClusterShardingSphereRepository::getProperties)
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .filter(e -> !map.containsKey(e.getKey()))
                    .map(e -> Tuple.tuple(e.getKey(), e.getValue()))))
            .toMap(Tuple2::v1, Tuple2::v2);
      case ZOO_KEEPER:
        var zooKeeper = Optional.of(repository)
            .map(StackGresShardedClusterShardingSphereRepository::getZooKeeper);
        return Seq.of(Seq.<Tuple2<String, String>>of()
            .append(zooKeeper
                .map(StackGresShardedClusterShardingSphereZooKeeper::getServerList)
                .map(value -> Tuple.tuple("server-lists", Seq.seq(value).toString(","))))
            .toMap(Tuple2::v1, Tuple2::v2))
            .flatMap(map -> Seq.seq(map)
                .append(Optional.of(repository)
                    .map(StackGresShardedClusterShardingSphereRepository::getProperties)
                    .stream()
                    .map(Map::entrySet)
                    .flatMap(Set::stream)
                    .filter(e -> !map.containsKey(e.getKey()))
                    .map(e -> Tuple.tuple(e.getKey(), e.getValue()))))
            .toMap(Tuple2::v1, Tuple2::v2);
      default:
        break;
    }
    return null;
  }

  static StackGresScript getCoordinatorScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    return
        new StackGresScriptBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.coordinatorScriptName(cluster))
            .build())
        .editSpec()
        .withScripts(
            getShardingSphereCreateDatabaseScript(context),
            getShardingSphereInitScript(context),
            getShardingSphereUpdateShardsScript(context))
        .endSpec()
        .build();
  }

  static StackGresScript getShardsScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    return
        new StackGresScriptBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.shardsScriptName(cluster))
            .build())
        .editSpec()
        .withScripts(
            getShardingSphereCreateDatabaseScript(context))
        .endSpec()
        .build();
  }

  private static StackGresScriptEntry getShardingSphereCreateDatabaseScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withName("shardingsphere-create-database")
        .withRetryOnError(true)
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(ClusterDefaultScripts.class.getResource(
                "/shardingsphere/shardingsphere-create-database.sql"),
                StandardCharsets.UTF_8)
            .read()).get()
            .formatted(cluster.getSpec().getDatabase()))
        .build();
    return script;
  }

  private static StackGresScriptEntry getShardingSphereInitScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withName("shardingsphere-init")
        .withRetryOnError(true)
        .withDatabase(cluster.getSpec().getDatabase())
        .withNewScriptFrom()
        .withNewSecretKeyRef()
        .withName(getInitSecretName(cluster))
        .withKey("shardingsphere-init.sql")
        .endCrdSecretKeyRef()
        .endScriptFrom()
        .build();
    return script;
  }

  static Secret getInitSecret(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    var superuserCredentials = ShardedClusterSecret.getSuperuserCredentials(context);
    final Secret secret = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(getInitSecretName(cluster))
        .endMetadata()
        .withData(ResourceUtil.encodeSecret(Map.of("shardingsphere-init.sql",
            Unchecked.supplier(() -> Resources
                .asCharSource(ClusterDefaultScripts.class.getResource(
                    "/shardingsphere/shardingsphere-init.sql"),
                    StandardCharsets.UTF_8)
                .read()).get().formatted(
                    StackGresShardedClusterUtil.primaryCoordinatorServiceName(cluster),
                    PatroniUtil.POSTGRES_SERVICE_PORT,
                    cluster.getSpec().getDatabase(),
                    superuserCredentials.v1,
                    superuserCredentials.v2))))
        .build();
    return secret;
  }

  private static StackGresScriptEntry getShardingSphereUpdateShardsScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withName("shardingsphere-update-shards")
        .withRetryOnError(true)
        .withDatabase(cluster.getSpec().getDatabase())
        .withNewScriptFrom()
        .withNewSecretKeyRef()
        .withName(getUpdateShardsSecretName(cluster))
        .withKey("shardingsphere-update-shards.sql")
        .endCrdSecretKeyRef()
        .endScriptFrom()
        .build();
    return script;
  }

  static Secret getUpdateShardsSecret(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    var superuserCredentials = ShardedClusterSecret.getSuperuserCredentials(context);
    final Secret secret = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(getUpdateShardsSecretName(cluster))
        .endMetadata()
        .withData(ResourceUtil.encodeSecret(Map.of("shardingsphere-update-shards.sql",
            Unchecked.supplier(() -> Resources
                .asCharSource(ClusterDefaultScripts.class.getResource(
                    "/shardingsphere/shardingsphere-update-shards.sql"),
                    StandardCharsets.UTF_8)
                .read()).get().formatted(
                    cluster.getSpec().getShards().getClusters(),
                    primaryShardServiceNamePlaceholder(cluster, "%1s"),
                    PatroniUtil.POSTGRES_SERVICE_PORT,
                    cluster.getSpec().getDatabase(),
                    superuserCredentials.v1,
                    superuserCredentials.v2))))
        .build();
    return secret;
  }

  static String getInitSecretName(StackGresShardedCluster cluster) {
    return StackGresShardedClusterUtil.coordinatorScriptName(cluster) + "-init";
  }

  static String getUpdateShardsSecretName(StackGresShardedCluster cluster) {
    return StackGresShardedClusterUtil.coordinatorScriptName(cluster) + "-update-shards";
  }

  private static String primaryShardServiceNamePlaceholder(
      StackGresShardedCluster cluster, String shardIndexPlaceholder) {
    return StackGresShardedClusterUtil.getShardClusterName(cluster, shardIndexPlaceholder);
  }

  static Role getShardingSphereOperatorRole(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    return
        new RoleBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.getCoordinatorClusterName(cluster))
            .build())
        .withRules(List.of(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(StackGresShardedCluster.class))
            .withResources(HasMetadata.getPlural(StackGresShardedCluster.class) + "/finalizers")
            .withVerbs("update")
            .build()))
        .build();
  }

  static RoleBinding getShardingSphereOperatorRoleBinding(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    var shardingSphereServiceAccount = Optional.of(context.getConfig().getSpec())
        .map(StackGresConfigSpec::getShardingSphere)
        .map(StackGresConfigShardingSphere::getServiceAccount)
        .orElseThrow(() -> new IllegalArgumentException(
            "You must configure the ShardingSphere operator"
                + " section in order to use shardingsphere sharding technology"));
    return
        new RoleBindingBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.getCoordinatorClusterName(cluster))
            .build())
        .withSubjects(List.of(new SubjectBuilder()
            .withApiGroup(HasMetadata.getGroup(ServiceAccount.class))
            .withKind(HasMetadata.getKind(ServiceAccount.class))
            .withNamespace(shardingSphereServiceAccount.getNamespace())
            .withName(shardingSphereServiceAccount.getName())
            .build()))
        .withNewRoleRef()
        .withApiGroup(HasMetadata.getGroup(Role.class))
        .withKind(HasMetadata.getKind(Role.class))
        .withName(StackGresShardedClusterUtil.getCoordinatorClusterName(cluster))
        .endRoleRef()
        .build();
  }

}
