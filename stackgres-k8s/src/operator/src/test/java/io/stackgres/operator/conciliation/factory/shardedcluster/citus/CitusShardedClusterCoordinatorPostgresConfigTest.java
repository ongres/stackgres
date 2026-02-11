/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.citus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operator.initialization.DefaultShardedClusterPostgresConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CitusShardedClusterCoordinatorPostgresConfigTest {

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private DefaultShardedClusterPostgresConfigFactory defaultPostgresConfigFactory;

  @Mock
  private StackGresShardedClusterContext context;

  private CitusShardedClusterCoordinatorPostgresConfig factory;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    factory = new CitusShardedClusterCoordinatorPostgresConfig(
        labelFactory, defaultPostgresConfigFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
  }

  @Test
  void generateResource_whenTypeCitus_shouldGenerateConfig() {
    cluster.getSpec().setType("citus");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCoordinatorPostgresConfig()).thenReturn(Optional.empty());
    when(defaultPostgresConfigFactory.buildResource(any()))
        .thenReturn(new StackGresPostgresConfigBuilder()
            .withNewSpec()
            .withPostgresqlConf(Map.of("max_connections", "100"))
            .endSpec()
            .build());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.getFirst() instanceof StackGresPostgresConfig);
  }

  @Test
  void generateResource_whenTypeNotCitus_shouldNotGenerateConfig() {
    cluster.getSpec().setType("ddp");
    when(context.getShardedCluster()).thenReturn(cluster);

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenTypeCitus_shouldIncludeCitusMaxClientConnections() {
    cluster.getSpec().setType("citus");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCoordinatorPostgresConfig()).thenReturn(Optional.empty());
    when(defaultPostgresConfigFactory.buildResource(any()))
        .thenReturn(new StackGresPostgresConfigBuilder()
            .withNewSpec()
            .withPostgresqlConf(Map.of("max_connections", "100"))
            .endSpec()
            .build());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertTrue(config.getSpec().getPostgresqlConf()
        .containsKey("citus.max_client_connections"));
  }

  @Test
  void generateResource_whenTypeCitusWithExistingConfig_shouldUseExistingConfig() {
    cluster.getSpec().setType("citus");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);

    StackGresPostgresConfig existingConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(Map.of(
            "max_connections", "200",
            "shared_preload_libraries", "citus"))
        .endSpec()
        .build();
    when(context.getCoordinatorPostgresConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertConfigHasLabels(config);
  }

  private void assertConfigHasLabels(StackGresPostgresConfig config) {
    assertTrue(config.getMetadata().getLabels() != null);
  }

}
