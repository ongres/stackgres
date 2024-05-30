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

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
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
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptBuilder;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShard;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.conciliation.factory.cluster.ClusterDefaultScripts;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
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
        spec.getConfigurations().setSgPostgresConfig(
            StackGresShardedClusterUtil.coordinatorConfigName(cluster));
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
    void updateShardsClusterSpec(StackGresShardedCluster cluster, StackGresClusterSpec spec, int index) {
      setConfigurationsPatroniInitialConfig(cluster, spec, index + 1);
    }

    private void setConfigurationsPatroniInitialConfig(
        StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
      if (spec.getConfigurations() == null) {
        spec.setConfigurations(new StackGresClusterConfigurations());
      }
      if (spec.getConfigurations().getPatroni() == null) {
        spec.getConfigurations().setPatroni(new StackGresClusterPatroni());
      }
      if (spec.getConfigurations().getPatroni().getInitialConfig() == null) {
        spec.getConfigurations().getPatroni()
            .setInitialConfig(new StackGresClusterPatroniConfig());
      }
      spec.getConfigurations().getPatroni().getInitialConfig()
          .put("scope", cluster.getMetadata().getName());
      var citus = new HashMap<String, Object>(2);
      citus.put("database", cluster.getSpec().getDatabase());
      citus.put("group", index);
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

    @Override
    void setOverridesLabels(StackGresShardedClusterShard specOverride, StackGresClusterSpec spec, int index) {
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

  static StackGresCluster getCoordinatorCluster(StackGresShardedCluster cluster) {
    return UTIL.getCoordinatorCluster(cluster);
  }

  static StackGresCluster getShardsCluster(StackGresShardedCluster cluster, int index) {
    return UTIL.getShardsCluster(cluster, index);
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
            .withName(StackGresShardedClusterUtil.coordinatorConfigName(cluster))
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
            getCitusUpdateShardsScript(context))
        .endSpec()
        .build();
  }

  private static StackGresScriptEntry getCitusUpdateShardsScript(
      StackGresShardedClusterContext context) {
    StackGresShardedCluster cluster = context.getShardedCluster();
    final StackGresScriptEntry script = new StackGresScriptEntryBuilder()
        .withName("citus-update-shards")
        .withRetryOnError(true)
        .withDatabase(cluster.getSpec().getDatabase())
        .withNewScriptFrom()
        .withNewSecretKeyRef()
        .withName(getUpdateShardsSecretName(cluster))
        .withKey("citus-update-shards.sql")
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
        .withData(ResourceUtil.encodeSecret(Map.of("citus-update-shards.sql",
            Unchecked.supplier(() -> Resources
                .asCharSource(ClusterDefaultScripts.class.getResource(
                    "/citus/citus-update-shards.sql"),
                    StandardCharsets.UTF_8)
                .read()).get().formatted(
                    superuserCredentials.v1,
                    "password=" + superuserCredentials.v2))))
        .build();
    return secret;
  }

  static String getUpdateShardsSecretName(StackGresShardedCluster cluster) {
    return StackGresShardedClusterUtil.coordinatorScriptName(cluster) + "-update-shards";
  }

}
