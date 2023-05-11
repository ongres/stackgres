/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfigurationBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurationBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniInitialConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServicesBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresCoordinatorServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresShardsServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShard;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

public interface StackGresShardedClusterForCitusUtil {

  static String getCoordinatorClusterName(StackGresShardedCluster cluster) {
    return cluster.getMetadata().getName() + "-coord";
  }

  static String getShardClusterName(StackGresShardedCluster cluster, int shardIndex) {
    return cluster.getMetadata().getName() + "-shard" + shardIndex;
  }

  static String primaryCoordinatorServiceName(StackGresShardedCluster cluster) {
    return primaryCoordinatorServiceName(cluster.getMetadata().getName());
  }

  static String primaryCoordinatorServiceName(String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName);
  }

  static String anyCoordinatorServiceName(StackGresShardedCluster cluster) {
    return ResourceUtil.nameIsValidService(cluster.getMetadata().getName() + "-reads");
  }

  static String primariesShardsServiceName(StackGresShardedCluster cluster) {
    return ResourceUtil.nameIsValidService(cluster.getMetadata().getName() + "-shards");
  }

  static String getClusterName(StackGresShardedCluster cluster, int index) {
    if (index == 0) {
      return getCoordinatorClusterName(cluster);
    }
    return getShardClusterName(cluster, index - 1);
  }

  static String coordinatorConfigName(StackGresShardedCluster cluster) {
    return cluster.getMetadata().getName() + "-coord";
  }

  static StackGresCluster getCoordinatorCluster(StackGresShardedCluster cluster) {
    final StackGresClusterSpec spec =
        new StackGresClusterSpecBuilder(cluster.getSpec().getCoordinator())
        .build();
    setClusterSpecFromShardedCluster(cluster, spec, 0);
    if (cluster.getSpec().getCoordinator().getReplicationForCoordinator() != null) {
      spec.setReplication(cluster.getSpec().getCoordinator().getReplicationForCoordinator());
    } else {
      spec.setReplication(cluster.getSpec().getReplication());
    }
    if (spec.getConfiguration() != null) {
      spec.setConfiguration(
          new StackGresClusterConfigurationBuilder(spec.getConfiguration())
          .build());
      spec.getConfiguration().setPostgresConfig(coordinatorConfigName(cluster));
    }
    StackGresCluster coordinatorCluster = new StackGresCluster();
    coordinatorCluster.setMetadata(new ObjectMeta());
    coordinatorCluster.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    coordinatorCluster.getMetadata().setName(getCoordinatorClusterName(cluster));
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
        .filter(specOverride -> specOverride.getIndex() == index)
        .findFirst()
        .ifPresent(specOverride -> setClusterSpecFromShardOverrides(
            specOverride, spec, index + 1));
    StackGresCluster shardsCluster = new StackGresCluster();
    shardsCluster.setMetadata(new ObjectMeta());
    shardsCluster.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    shardsCluster.getMetadata().setName(getShardClusterName(cluster, index));
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
    setExtensions(cluster, spec);
    setBackups(cluster, spec, index);
    setCredentials(cluster, spec);
    setPatroniInitialConfig(cluster, spec, index);
    setMetadata(cluster, spec, index);
    spec.setDistributedLogs(cluster.getSpec().getDistributedLogs());
    spec.setPrometheusAutobind(cluster.getSpec().getPrometheusAutobind());
    spec.setNonProductionOptions(cluster.getSpec().getNonProductionOptions());
  }

  private static void setBackups(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    Optional.ofNullable(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getConfiguration)
        .map(StackGresShardedClusterConfiguration::getBackups)
        .filter(Predicate.not(List::isEmpty))
        .map(backups -> backups.get(0))
        .filter(backup -> backup.getPaths() != null)
        .ifPresent(backup -> {
          if (spec.getConfiguration() == null) {
            spec.setConfiguration(new StackGresClusterConfiguration());
          }
          spec.getConfiguration().setBackups(List.of(
              new StackGresClusterBackupConfigurationBuilder()
              .withObjectStorage(backup.getObjectStorage())
              .withPath(backup.getPaths().get(index))
              .withCronSchedule(backup.getCronSchedule())
              .withRetention(backup.getRetention())
              .withCompression(backup.getCompression())
              .withPerformance(backup.getPerformance())
              .build()));
        });
  }

  private static void setExtensions(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec) {
    spec.setPostgres(
        new StackGresClusterPostgresBuilder(cluster.getSpec().getPostgres())
        .withExtensions(Optional.ofNullable(cluster.getStatus())
            .map(StackGresShardedClusterStatus::getToInstallPostgresExtensions)
            .stream()
            .flatMap(List::stream)
            .map(extension -> new StackGresClusterExtensionBuilder()
                .withName(extension.getName())
                .withPublisher(extension.getPublisher())
                .withRepository(extension.getRepository())
                .withVersion(extension.getVersion())
                .build())
            .toList())
        .build());
    if (cluster.getStatus() != null) {
      spec.setToInstallPostgresExtensions(cluster.getStatus().getToInstallPostgresExtensions());
    }
  }

  private static void setCredentials(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec) {
    spec.getConfiguration().setCredentials(new StackGresClusterCredentials());
    spec.getConfiguration().getCredentials()
        .setPatroni(new StackGresClusterPatroniCredentials());
    spec.getConfiguration().getCredentials().getPatroni()
        .setRestApiPassword(new SecretKeySelector(
            StackGresPasswordKeys.RESTAPI_PASSWORD_KEY,
            cluster.getMetadata().getName()));
    spec.getConfiguration().getCredentials()
        .setUsers(new StackGresClusterUsersCredentials());
    spec.getConfiguration().getCredentials().getUsers()
        .setSuperuser(new StackGresClusterUserSecretKeyRef());
    spec.getConfiguration().getCredentials().getUsers().getSuperuser()
        .setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY,
            cluster.getMetadata().getName()));
    spec.getConfiguration().getCredentials().getUsers()
        .setReplication(new StackGresClusterUserSecretKeyRef());
    spec.getConfiguration().getCredentials().getUsers().getReplication()
        .setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_KEY,
            cluster.getMetadata().getName()));
    spec.getConfiguration().getCredentials().getUsers()
        .setAuthenticator(new StackGresClusterUserSecretKeyRef());
    spec.getConfiguration().getCredentials().getUsers().getAuthenticator()
        .setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_KEY,
            cluster.getMetadata().getName()));
  }

  private static void setPatroniInitialConfig(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    spec.getConfiguration().setPatroni(new StackGresClusterPatroni());
    spec.getConfiguration().getPatroni()
        .setInitialConfig(new StackGresClusterPatroniInitialConfig());
    spec.getConfiguration().getPatroni().getInitialConfig()
        .put("scope", cluster.getMetadata().getName());
    var citus = new HashMap<String, Object>(2);
    citus.put("database", cluster.getSpec().getDatabase());
    citus.put("group", index);
    spec.getConfiguration().getPatroni().getInitialConfig()
        .put("citus", citus);
  }

  private static void setMetadata(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    if (spec.getMetadata() == null) {
      spec.setMetadata(new StackGresClusterSpecMetadata());
    }
    setLabels(cluster, spec, index);
    if (spec.getMetadata().getAnnotations() == null
        && cluster.getSpec().getMetadata() != null
        && cluster.getSpec().getMetadata().getAnnotations() != null) {
      spec.getMetadata().setAnnotations(cluster.getSpec().getMetadata().getAnnotations());
    }
  }

  private static void setLabels(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    if (spec.getMetadata().getLabels() == null) {
      spec.getMetadata().setLabels(new StackGresClusterSpecLabels());
    }
    var specLabels = spec.getMetadata().getLabels();
    var clusterLabels = Optional.of(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getLabels)
        .orElseGet(() -> new StackGresClusterSpecLabels());
    if (specLabels.getClusterPods() != null) {
      specLabels.setClusterPods(
          withCitusGroupLabel(specLabels.getClusterPods(), index));
    } else {
      specLabels.setClusterPods(
          withCitusGroupLabel(clusterLabels.getClusterPods(), index));
    }
    if (specLabels.getServices() != null) {
      specLabels.setServices(
          withCitusGroupLabel(specLabels.getServices(), index));
    } else {
      specLabels.setServices(
          withCitusGroupLabel(clusterLabels.getServices(), index));
    }
  }

  private static Map<String, String> withCitusGroupLabel(Map<String, String> labels, int index) {
    return mergeMaps(
        labels,
        Map.entry("citus-group", String.valueOf(index)));
  }

  private static Map<String, String> mergeMaps(
      Map<String, String> spec,
      Map.Entry<String, String> extraEntry) {
    return Seq.seq(Optional.ofNullable(spec))
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .filter(e -> !Objects.equals(extraEntry.getKey(), e.getKey()))
        .append(extraEntry)
        .toMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  private static void setClusterSpecFromShardOverrides(
      StackGresShardedClusterShard specOverride, StackGresClusterSpec spec, int index) {
    if (specOverride.getConfigurationForShards() != null) {
      if (specOverride.getConfigurationForShards().getPostgresConfig() != null) {
        spec.getConfiguration().setPostgresConfig(
            specOverride.getConfigurationForShards().getPostgresConfig());
      }
      if (specOverride.getConfigurationForShards().getConnectionPoolingConfig() != null) {
        spec.getConfiguration().setConnectionPoolingConfig(
            specOverride.getConfigurationForShards().getConnectionPoolingConfig());
      }
    }
    if (specOverride.getInstancesPerCluster() != null) {
      spec.setInstances(specOverride.getInstancesPerCluster());
    }
    if (specOverride.getResourceProfile() != null) {
      spec.setResourceProfile(specOverride.getResourceProfile());
    }
    if (specOverride.getReplicationForShards() != null) {
      spec.setReplication(specOverride.getReplicationForShards());
    }
    if (specOverride.getManagedSql() != null) {
      spec.setManagedSql(specOverride.getManagedSql());
    }
    if (specOverride.getMetadata() != null) {
      if (spec.getMetadata() == null) {
        spec.setMetadata(new StackGresClusterSpecMetadata());
      }
      if (specOverride.getMetadata().getLabels() != null) {
        if (spec.getMetadata().getLabels() == null) {
          spec.getMetadata().setLabels(new StackGresClusterSpecLabels());
        }
        if (specOverride.getMetadata().getLabels().getClusterPods() != null) {
          spec.getMetadata().getLabels().setClusterPods(
              withCitusGroupLabel(specOverride.getMetadata().getLabels().getClusterPods(), index));
        }
        if (specOverride.getMetadata().getLabels().getServices() != null) {
          spec.getMetadata().getLabels().setServices(
              withCitusGroupLabel(specOverride.getMetadata().getLabels().getServices(), index));
        }
      }
      if (specOverride.getMetadata().getAnnotations() != null) {
        spec.getMetadata().setAnnotations(specOverride.getMetadata().getAnnotations());
      }
    }
    if (specOverride.getPodForShards() != null) {
      if (specOverride.getPodForShards().getDisableConnectionPooling() != null) {
        spec.getPod().setDisableConnectionPooling(
            specOverride.getPodForShards().getDisableConnectionPooling());
      }
      if (specOverride.getPodForShards().getDisableMetricsExporter() != null) {
        spec.getPod().setDisableMetricsExporter(
            specOverride.getPodForShards().getDisableMetricsExporter());
      }
      if (specOverride.getPodForShards().getDisablePostgresUtil() != null) {
        spec.getPod().setDisablePostgresUtil(
            specOverride.getPodForShards().getDisablePostgresUtil());
      }
      if (specOverride.getPodForShards().getManagementPolicy() != null) {
        spec.getPod().setManagementPolicy(specOverride.getPodForShards().getManagementPolicy());
      }
      if (specOverride.getPodForShards().getPersistentVolume() != null) {
        if (specOverride.getPodForShards().getPersistentVolume().getSize() != null) {
          spec.getPod().getPersistentVolume().setSize(
              specOverride.getPodForShards().getPersistentVolume().getSize());
        }
        if (specOverride.getPodForShards().getPersistentVolume().getStorageClass() != null) {
          spec.getPod().getPersistentVolume().setStorageClass(
              specOverride.getPodForShards().getPersistentVolume().getStorageClass());
        }
      }
      if (specOverride.getPodForShards().getResources() != null) {
        if (spec.getPod().getResources() == null) {
          spec.getPod().setResources(new StackGresClusterResources());
        }
        if (specOverride.getPodForShards().getResources()
            .getEnableClusterLimitsRequirements() != null) {
          spec.getPod().getResources().setEnableClusterLimitsRequirements(
              specOverride.getPodForShards().getResources().getEnableClusterLimitsRequirements());
        }
      }
      if (specOverride.getPodForShards().getScheduling() != null) {
        spec.getPod().setScheduling(
            specOverride.getPodForShards().getScheduling());
      }
      if (specOverride.getPodForShards().getCustomVolumes() != null) {
        spec.getPod().setCustomVolumes(specOverride.getPodForShards().getCustomVolumes());
      }
      if (specOverride.getPodForShards().getCustomContainers() != null) {
        spec.getPod().setCustomContainers(specOverride.getPodForShards().getCustomContainers());
      }
      if (specOverride.getPodForShards().getCustomInitContainers() != null) {
        spec.getPod().setCustomInitContainers(
            specOverride.getPodForShards().getCustomInitContainers());
      }
    }
  }

  static StackGresPostgresConfig getCoordinatorPostgresConfig(
      StackGresShardedCluster cluster, StackGresPostgresConfig coordinatorPostgresConfig) {
    Map<String, String> postgresqlConf =
        coordinatorPostgresConfig.getSpec().getPostgresqlConf();
    Integer maxConnections = Optional.ofNullable(postgresqlConf.get("max_connections"))
        .map(Integer::parseInt)
        .orElse(100);
    int workers = cluster.getSpec().getShards().getClusters();
    Map<String, String> computedParameters = Map.of("citus.max_client_connections",
        String.valueOf(
            maxConnections * 90 / (100 * (1 + workers))
            ));
    return
        new StackGresPostgresConfigBuilder(coordinatorPostgresConfig)
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(coordinatorConfigName(cluster))
            .build())
        .editSpec()
        .withPostgresqlConf(Seq.seq(postgresqlConf)
            .append(Seq.seq(computedParameters)
                .filter(t -> !postgresqlConf.containsKey(t.v1)))
            .toMap(Tuple2::v1, Tuple2::v2))
        .endSpec()
        .withStatus(null)
        .build();
  }

}
