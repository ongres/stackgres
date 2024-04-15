/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurationsBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShard;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import org.jooq.lambda.Seq;
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
}
