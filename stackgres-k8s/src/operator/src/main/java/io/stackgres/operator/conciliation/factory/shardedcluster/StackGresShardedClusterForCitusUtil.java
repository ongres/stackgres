/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.ClusterPathV1;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurationsBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptBuilder;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
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
          Map.entry("citus-group", String.valueOf(index)));
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
            getCitusUpdateQueryRoutersScript(context, 1))
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
        ClusterPathV1.PG_RUN_PATH.path(),
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

  private static StackGresScriptEntry getCitusUpdateQueryRoutersScript(
      StackGresShardedClusterContext context, int id) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final int queryRoutersStartIndex = Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getQueryRouterIndexOffset)
        .orElse(1024);
    final int queryRoutersEndIndex = queryRoutersStartIndex
        + Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getQueryRouterClusters)
        .orElse(0);
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withId(id)
        .withName("citus-update-query-routers")
        .withRetryOnError(true)
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(StackGresShardedClusterForCitusUtil.class.getResource(
                "/citus/citus-update-query-routers.sql"),
                StandardCharsets.UTF_8)
            .read()).get().formatted(
                String.valueOf(queryRoutersStartIndex),
                String.valueOf(queryRoutersEndIndex),
                DSL.inline(cluster.getSpec().getDatabase())))
        .build();
    return script;
  }

  static String getUpdateWorkersSecretName(StackGresShardedCluster cluster) {
    return StackGresShardedClusterUtil.coordinatorScriptName(cluster) + "-update-workers";
  }

}
