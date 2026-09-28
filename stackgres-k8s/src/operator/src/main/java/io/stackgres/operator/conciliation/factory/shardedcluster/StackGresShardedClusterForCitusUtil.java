/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.ManagedSqlCronUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurationsBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryScriptStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptBuilder;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCitusConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecLabels;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecMetadata;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.impl.DSL;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple2;

public interface StackGresShardedClusterForCitusUtil extends StackGresShardedClusterUtil {

  Util UTIL = new Util();

  int QUERY_ROUTERS_WITHOUT_SHARDS_SCRIPT_ID = 3;

  class Util extends StackGresShardedClusterForUtil {

    @Override
    void updateCoordinatorSpec(StackGresShardedCluster cluster, StackGresClusterSpec spec) {
      if (spec.getConfigurations() != null) {
        spec.setConfigurations(
            new StackGresClusterConfigurationsBuilder(spec.getConfigurations())
            .build());
      }
      setConfigurationsPatroniInitialConfig(cluster, spec, 0);
      if (spec.getManagedSql() == null) {
        spec.setManagedSql(new StackGresClusterManagedSql());
      }
      spec.getManagedSql().setScripts(
          Seq.seq(Optional.ofNullable(spec.getManagedSql().getScripts())
              .stream()
              .flatMap(List::stream)
              .limit(1))
          .append(new StackGresClusterManagedScriptEntryBuilder()
              .withSgScript(StackGresShardedClusterUtil.coordinatorScriptName(cluster))
              .withId(1)
              .build())
          .append(Optional.ofNullable(spec.getManagedSql().getScripts())
              .stream()
              .flatMap(List::stream)
              .skip(1))
          .toList());
    }

    @Override
    void updateWorkerClusterSpec(StackGresShardedCluster cluster, StackGresClusterSpec spec, int index) {
      setConfigurationsPatroniInitialConfig(cluster, spec, index + 1);
    }

    @Override
    void updateQueryRouterClusterSpec(StackGresShardedCluster cluster, StackGresClusterSpec spec, int index) {
      setConfigurationsPatroniInitialConfig(cluster, spec, index + 1);
      spec.getConfigurations().getPatroni().setStartGateAnnotations(mergeMaps(
          spec.getConfigurations().getPatroni().getStartGateAnnotations(),
          Map.entry(StackGresContext.CITUS_GROUP_REGISTERED_ANNOTATION, String.valueOf(index + 1))));
    }

    @Override
    String getCoordinatorPostgresConfig(StackGresShardedCluster cluster) {
      return StackGresShardedClusterUtil.coordinatorConfigName(cluster);
    }

    @Override
    String getWorkerPostgresConfig(StackGresShardedCluster cluster, int index) {
      return StackGresShardedClusterUtil.workerConfigName(cluster, index);
    }

    @Override
    String getQueryRouterPostgresConfig(StackGresShardedCluster cluster, int index) {
      return StackGresShardedClusterUtil.queryRouterConfigName(cluster, index);
    }

    private void setConfigurationsPatroniInitialConfig(
        StackGresShardedCluster cluster, final StackGresClusterSpec spec, int globalIndex) {
      if (spec.getConfigurations() == null) {
        spec.setConfigurations(new StackGresClusterConfigurations());
      }
      StackGresClusterPatroni patroni = spec.getConfigurations().getPatroni();
      spec.getConfigurations().setPatroni(new StackGresClusterPatroni());
      if (patroni == null) {
        patroni = new StackGresClusterPatroni();
      }
      spec.getConfigurations().getPatroni().setConnectUsingFqdn(patroni.getConnectUsingFqdn());
      spec.getConfigurations().getPatroni().setDynamicConfig(patroni.getDynamicConfig());
      spec.getConfigurations().getPatroni().setStartGateAnnotations(patroni.getStartGateAnnotations());
      if (patroni.getInitialConfig() == null) {
        spec.getConfigurations().getPatroni()
            .setInitialConfig(new StackGresClusterPatroniConfig());
      } else {
        spec.getConfigurations().getPatroni()
            .setInitialConfig(
                new StackGresClusterPatroniConfig(
                    patroni.getInitialConfig().deepCopy()));
      }
      spec.getConfigurations().getPatroni().getInitialConfig()
          .put("scope", cluster.getMetadata().getName());
      var citus = new HashMap<String, Object>(2);
      citus.put("database", cluster.getSpec().getDatabase());
      citus.put("group", globalIndex);
      spec.getConfigurations().getPatroni().getInitialConfig()
          .put("citus", citus);
    }

    @Override
    void setLabels(
        StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
      if (spec.getMetadata().getLabels() == null) {
        spec.getMetadata().setLabels(new StackGresClusterSpecLabels());
      }
      var specLabels = spec.getMetadata().getLabels();
      var clusterLabels = Optional.of(cluster.getSpec())
          .map(StackGresShardedClusterSpec::getMetadata)
          .map(StackGresShardedClusterSpecMetadata::getLabels)
          .orElseGet(() -> new StackGresShardedClusterSpecLabels());
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

    @Override
    void setOverridesLabels(StackGresShardedClusterWorker specOverride, StackGresClusterSpec spec, int index) {
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
    }

    private Map<String, String> withCitusGroupLabel(Map<String, String> labels, int index) {
      return mergeMaps(
          labels,
          Map.entry(StackGresContext.CITUS_GROUP_KEY, String.valueOf(index)));
    }
  }

  static StackGresCluster getCoordinatorCluster(
      StackGresShardedCluster cluster,
      Optional<StackGresShardedCluster> replicateCluster) {
    return UTIL.getBaseCoordinatorCluster(cluster, replicateCluster);
  }

  static StackGresCluster getWorkerCluster(
      StackGresShardedCluster cluster,
      int index,
      Optional<StackGresShardedCluster> replicateCluster) {
    return UTIL.getBaseWorkerCluster(cluster, index, replicateCluster);
  }

  static StackGresCluster getQueryRouterCluster(
      StackGresShardedCluster cluster,
      int index,
      Optional<StackGresShardedCluster> replicateCluster) {
    return UTIL.getBaseQueryRouterCluster(cluster, index, replicateCluster);
  }

  static StackGresPostgresConfig getCoordinatorPostgresConfig(
      StackGresShardedCluster cluster, StackGresPostgresConfig coordinatorPostgresConfig) {
    return getCitusPostgresConfig(
        cluster,
        StackGresShardedClusterUtil.coordinatorConfigName(cluster),
        coordinatorPostgresConfig);
  }

  static StackGresScript getCoordinatorScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    return new StackGresScriptBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(StackGresShardedClusterUtil.coordinatorScriptName(cluster))
            .build())
        .editSpec()
        .withScripts(
            getCitusUpdateWorkersScript(context, 0),
            getCitusRemovePgCronJobsScript(context, 1),
            getCitusUpdateNodesScript(context, 2),
            getCitusQueryRoutersWithoutShardsScript(context, QUERY_ROUTERS_WITHOUT_SHARDS_SCRIPT_ID))
        .endSpec()
        .build();
  }

  static StackGresPostgresConfig getWorkerPostgresConfig(
      StackGresShardedCluster cluster,
      int index,
      StackGresPostgresConfig workerPostgresConfig) {
    return getCitusPostgresConfig(
        cluster,
        StackGresShardedClusterUtil.workerConfigName(cluster, index),
        workerPostgresConfig);
  }

  static StackGresPostgresConfig getQueryRouterPostgresConfig(
      StackGresShardedCluster cluster,
      int index,
      StackGresPostgresConfig queryRouterPostgresConfig) {
    return getCitusPostgresConfig(
        cluster,
        StackGresShardedClusterUtil.queryRouterConfigName(cluster, index),
        queryRouterPostgresConfig);
  }

  private static StackGresPostgresConfig getCitusPostgresConfig(
      StackGresShardedCluster cluster,
      String configName,
      StackGresPostgresConfig postgresConfig) {
    Map<String, String> postgresqlConf =
        postgresConfig.getSpec().getPostgresqlConf();
    final String sharedPreloadLibraries =
        Optional.ofNullable(postgresqlConf.get("shared_preload_libraries"))
        .orElse("");
    final String spaceSeparatedSharedPreloadLibraries =
        sharedPreloadLibraries
        .replace(',', ' ');
    Map<String, String> computedParameters = Map.of();
    Map<String, String> overwrittenParameters = Map.of(
        "cron.database_name",
        "postgres",
        "cron.host",
        ClusterPath.PG_RUN_PATH.path(),
        "shared_preload_libraries",
        Seq.of("citus", "pg_cron")
        .append(Seq.of(spaceSeparatedSharedPreloadLibraries.split(" +"))
            .filter(Predicate.not(String::isEmpty))
            .filter(Predicate.not(String::isBlank))
            .filter(Predicate.not(List.of("citus", "pg_cron")::contains)))
        .collect(Collectors.joining(", ")));
    return
        new StackGresPostgresConfigBuilder(postgresConfig)
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(configName)
            .build())
        .editSpec()
        .withPostgresqlConf(Seq.seq(postgresqlConf)
            .append(Seq.seq(computedParameters)
                .filter(t -> !postgresqlConf.containsKey(t.v1)))
            .filter(t -> !overwrittenParameters.containsKey(t.v1))
            .append(Seq.seq(overwrittenParameters))
            .toMap(Tuple2::v1, Tuple2::v2))
        .endSpec()
        .withStatus(null)
        .build();
  }

  private static StackGresScriptEntry getCitusUpdateWorkersScript(
      StackGresShardedClusterContext context, int id) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("citus-update-workers")
        .withRetryOnError(true)
        .withDatabase(cluster.getSpec().getDatabase())
        .withNewScriptFrom()
        .withNewSecretKeyRef()
        .withName(getUpdateWorkersSecretName(cluster))
        .withKey("citus-update-workers.sql")
        .endSecretKeyRef()
        .endScriptFrom()
        .build();
    return script;
  }

  static Secret getUpdateWorkersSecret(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    var superuserCredentials = ShardedClusterSecret.getSuperuserCredentials(context);
    final Secret secret = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(getUpdateWorkersSecretName(cluster))
        .endMetadata()
        .withData(ResourceUtil.encodeSecret(Map.of("citus-update-workers.sql",
            Unchecked.supplier(() -> Resources
                .asCharSource(StackGresShardedClusterForCitusUtil.class.getResource(
                    "/citus/citus-update-workers.sql"),
                    StandardCharsets.UTF_8)
                .read()).get().formatted(
                    DSL.inline(superuserCredentials.v1),
                    DSL.inline("password=" + DSL.inline(superuserCredentials.v2))))))
        .build();
    return secret;
  }

  private static StackGresScriptEntry getCitusRemovePgCronJobsScript(
      StackGresShardedClusterContext context, int id) {
    return new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("citus-remove-pg-cron-jobs")
        .withRetryOnError(true)
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(StackGresShardedClusterForCitusUtil.class.getResource(
                "/citus/citus-remove-pg-cron-jobs.sql"),
                StandardCharsets.UTF_8)
            .read()).get())
        .build();
  }

  private static StackGresScriptEntry getCitusUpdateNodesScript(
      StackGresShardedClusterContext context, int id) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    return new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("citus-update-nodes")
        .withDatabase(cluster.getSpec().getDatabase())
        .withCron(getUpdateNodeCron(cluster))
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(StackGresShardedClusterForCitusUtil.class.getResource(
                "/citus/citus-update-nodes.sql"),
                StandardCharsets.UTF_8)
            .read()).get().formatted(
                String.valueOf(getQueryRoutersIndexOffset(cluster)),
                String.valueOf(getQueryRoutersEndIndex(cluster)),
                String.valueOf(cluster.getSpec().getWorkers().getClusters()),
                String.valueOf(getCitusConfigurations(cluster)
                    .map(StackGresShardedClusterCitusConfigurations::getEnableNodeAutoRemovalOrDefault)
                    .orElse(false))))
        .build();
  }

  private static StackGresScriptEntry getCitusQueryRoutersWithoutShardsScript(
      StackGresShardedClusterContext context, int id) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    return new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("citus-query-routers-without-shards")
        .withDatabase(cluster.getSpec().getDatabase())
        .withCron(getUpdateNodeCron(cluster))
        .withSetValue(true)
        .withScript("SELECT string_agg(groupid::text, ',' ORDER BY groupid)"
            + " FROM pg_catalog.pg_dist_node"
            + " WHERE noderole = 'primary' AND NOT shouldhaveshards"
            + " AND groupid > " + getQueryRoutersIndexOffset(cluster))
        .build();
  }

  private static Optional<StackGresShardedClusterCitusConfigurations> getCitusConfigurations(
      StackGresShardedCluster cluster) {
    return Optional.of(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getCitus);
  }

  private static String getUpdateNodeCron(StackGresShardedCluster cluster) {
    return ManagedSqlCronUtil.everyInterval(getCitusConfigurations(cluster)
        .map(StackGresShardedClusterCitusConfigurations::getUpdateNodeIntervalOrDefault)
        .orElse(StackGresShardedClusterCitusConfigurations.DEFAULT_UPDATE_NODE_INTERVAL));
  }

  private static int getQueryRoutersIndexOffset(StackGresShardedCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getQueryRouterIndexOffset)
        .orElse(1024);
  }

  private static int getQueryRoutersEndIndex(StackGresShardedCluster cluster) {
    return getQueryRoutersIndexOffset(cluster)
        + Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getQueryRouterClusters)
        .orElse(0);
  }

  /**
   * Return {@code true} if the Patroni of the query router of the specified Citus group can be
   * started: the coordinator SGScript returned the group among those registered in
   * {@code pg_dist_node} without shards. The cluster-controller of the query router Pods and of the
   * coordinator Pods are only upgraded when their Pods are restarted, so a coordinator that does not
   * support the SGScript fields {@code setValue} and {@code cron} yet will eventually report the
   * group once restarted, allowing the query router to start.
   */
  static boolean isQueryRouterRegistered(StackGresShardedClusterContext context, int group) {
    final Optional<StackGresCluster> deployedCoordinator = context.getDeployedCoordinator();
    final String coordinatorScriptName =
        StackGresShardedClusterUtil.coordinatorScriptName(context.getShardedCluster());
    final Optional<Integer> coordinatorScriptId = deployedCoordinator
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getManagedSql)
        .map(StackGresClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream)
        .filter(managedScript -> coordinatorScriptName.equals(managedScript.getSgScript()))
        .map(StackGresClusterManagedScriptEntry::getId)
        .findFirst();
    return deployedCoordinator
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getManagedSql)
        .map(StackGresClusterManagedSqlStatus::getScripts)
        .stream()
        .flatMap(List::stream)
        .filter(managedScriptStatus -> coordinatorScriptId
            .filter(managedScriptStatus.getId()::equals).isPresent())
        .map(StackGresClusterManagedScriptEntryStatus::getScripts)
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .filter(scriptStatus -> Objects.equals(
            QUERY_ROUTERS_WITHOUT_SHARDS_SCRIPT_ID, scriptStatus.getId()))
        .map(StackGresClusterManagedScriptEntryScriptStatus::getValue)
        .filter(Objects::nonNull)
        .flatMap(value -> Stream.of(value.split(",")))
        .anyMatch(String.valueOf(group)::equals);
  }

  static String getUpdateWorkersSecretName(StackGresShardedCluster cluster) {
    return StackGresShardedClusterUtil.coordinatorScriptName(cluster) + "-update-workers";
  }

}
