/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterShardsDefaultPostgresConfigTest {

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private DefaultShardedClusterPostgresConfigFactory defaultFactory;

  @Mock
  private StackGresShardedClusterContext context;

  private ShardedClusterShardsDefaultPostgresConfig factory;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    factory = new ShardedClusterShardsDefaultPostgresConfig(labelFactory, defaultFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);

    lenient().when(defaultFactory.buildResource(any()))
        .thenReturn(new StackGresPostgresConfigBuilder().withNewSpec().endSpec().build());
  }

  @Test
  void generateResource_whenConfigEmpty_shouldGenerateDefault() {
    cluster.getSpec().getShards().getConfigurations()
        .setSgPostgresConfig("shards-postgresconf");
    when(context.getShardsPostgresConfig()).thenReturn(Optional.empty());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals(
        cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig(),
        config.getMetadata().getName());
  }

  @Test
  void generateResource_whenConfigExistsWithDefaultLabelsAndOwner_shouldGenerate() {
    cluster.getSpec().getShards().getConfigurations()
        .setSgPostgresConfig("shards-postgresconf");
    StackGresPostgresConfig existingConfig = new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withLabels(Map.copyOf(labelFactory.defaultConfigLabels(cluster)))
        .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
        .endMetadata()
        .build();
    when(context.getShardsPostgresConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals(
        cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig(),
        config.getMetadata().getName());
  }

  @Test
  void generateResource_whenConfigExistsWithoutMatchingLabels_shouldNotGenerate() {
    cluster.getSpec().getShards().getConfigurations()
        .setSgPostgresConfig("shards-postgresconf");
    StackGresPostgresConfig existingConfig = new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withLabels(Map.of("other-label", "other-value"))
        .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
        .endMetadata()
        .build();
    when(context.getShardsPostgresConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenSameAsCoordinatorConfig_shouldNotGenerate() {
    lenient().when(context.getShardsPostgresConfig()).thenReturn(Optional.empty());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

}
