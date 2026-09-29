/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.citus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operator.initialization.DefaultShardedClusterPostgresConfigFactory;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CitusShardedClusterQueryRouterPostgresConfigTest {

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private DefaultShardedClusterPostgresConfigFactory defaultPostgresConfigFactory;

  @Mock
  private StackGresShardedClusterContext context;

  private CitusShardedClusterQueryRouterPostgresConfig factory;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    factory = new CitusShardedClusterQueryRouterPostgresConfig(
        labelFactory, defaultPostgresConfigFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    lenient().when(defaultPostgresConfigFactory.buildResource(any()))
        .thenReturn(new StackGresPostgresConfigBuilder()
            .withNewSpec()
            .withPostgresqlConf(Map.of("max_connections", "100"))
            .endSpec()
            .build());
  }

  @Test
  void generateResource_whenTypeCitus_shouldGenerateOneConfigPerQueryRouter() {
    cluster.getSpec().setType("citus");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    StackGresPostgresConfig existingConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(Map.of(
            "max_connections", "200",
            "shared_preload_libraries", "pg_stat_statements"))
        .endSpec()
        .build();
    when(context.getQueryRoutersPostgresConfigs()).thenReturn(List.of(
        Tuple.tuple(1024, Optional.of(existingConfig)),
        Tuple.tuple(1025, Optional.of(existingConfig))));

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(2, resources.size());
    StackGresPostgresConfig config0 = (StackGresPostgresConfig) resources.get(0);
    StackGresPostgresConfig config1 = (StackGresPostgresConfig) resources.get(1);
    assertEquals(
        StackGresShardedClusterUtil.queryRouterConfigName(cluster, 1024),
        config0.getMetadata().getName());
    assertEquals(
        StackGresShardedClusterUtil.queryRouterConfigName(cluster, 1025),
        config1.getMetadata().getName());
    assertEquals("citus, pg_cron, pg_stat_statements",
        config0.getSpec().getPostgresqlConf().get("shared_preload_libraries"));
    assertEquals("200", config0.getSpec().getPostgresqlConf().get("max_connections"));
    assertEquals(labelFactory.queryRoutersLabels(cluster), config0.getMetadata().getLabels());
    assertEquals(cluster.getMetadata().getNamespace(), config0.getMetadata().getNamespace());
  }

  @Test
  void generateResource_whenQueryRouterConfigMissing_shouldUseDefaultConfig() {
    cluster.getSpec().setType("citus");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    when(context.getQueryRoutersPostgresConfigs()).thenReturn(List.of(
        Tuple.tuple(1024, Optional.empty())));

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals(
        StackGresShardedClusterUtil.queryRouterConfigName(cluster, 1024),
        config.getMetadata().getName());
    assertEquals("100", config.getSpec().getPostgresqlConf().get("max_connections"));
    assertTrue(config.getSpec().getPostgresqlConf().get("shared_preload_libraries")
        .startsWith("citus, pg_cron"));
  }

  @Test
  void generateResource_whenOnlyOneQueryRouterIsOverridden_shouldUseItsOwnConfig() {
    cluster.getSpec().setType("citus");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    StackGresPostgresConfig coordinatorConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(Map.of("max_connections", "200"))
        .endSpec()
        .build();
    StackGresPostgresConfig overriddenConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(Map.of("max_connections", "400"))
        .endSpec()
        .build();
    when(context.getQueryRoutersPostgresConfigs()).thenReturn(List.of(
        Tuple.tuple(1024, Optional.of(overriddenConfig)),
        Tuple.tuple(1025, Optional.of(coordinatorConfig))));

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(2, resources.size());
    StackGresPostgresConfig config0 = (StackGresPostgresConfig) resources.get(0);
    StackGresPostgresConfig config1 = (StackGresPostgresConfig) resources.get(1);
    assertEquals("400", config0.getSpec().getPostgresqlConf().get("max_connections"));
    assertEquals("200", config1.getSpec().getPostgresqlConf().get("max_connections"));
  }

  @Test
  void generateResource_whenTypeNotCitus_shouldNotGenerateConfig() {
    cluster.getSpec().setType("ddp");
    when(context.getShardedCluster()).thenReturn(cluster);

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

}
