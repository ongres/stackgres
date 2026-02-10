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
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operator.initialization.DefaultPoolingConfigFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterCoordinatorPgBouncerDefaultPoolingConfigTest {

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private DefaultPoolingConfigFactory defaultPoolingConfigFactory;

  @Mock
  private StackGresShardedClusterContext context;

  private ShardedClusterCoordinatorPgBouncerDefaultPoolingConfig factory;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    factory = new ShardedClusterCoordinatorPgBouncerDefaultPoolingConfig(
        labelFactory, defaultPoolingConfigFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);

    lenient().when(defaultPoolingConfigFactory.buildResource(any()))
        .thenReturn(new StackGresPoolingConfigBuilder().withNewSpec().endSpec().build());
  }

  @Test
  void generateResource_whenPoolingConfigEmpty_shouldGenerateDefault() {
    when(context.getCoordinatorPoolingConfig()).thenReturn(Optional.empty());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPoolingConfig config = (StackGresPoolingConfig) resources.getFirst();
    assertEquals(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig(),
        config.getMetadata().getName());
  }

  @Test
  void generateResource_whenPoolingConfigExistsWithDefaultLabelsAndOwner_shouldGenerate() {
    StackGresPoolingConfig existingConfig = new StackGresPoolingConfigBuilder()
        .withNewMetadata()
        .withLabels(Map.copyOf(labelFactory.defaultConfigLabels(cluster)))
        .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
        .endMetadata()
        .build();
    when(context.getCoordinatorPoolingConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPoolingConfig config = (StackGresPoolingConfig) resources.getFirst();
    assertEquals(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig(),
        config.getMetadata().getName());
  }

  @Test
  void generateResource_whenPoolingConfigExistsWithoutMatchingLabels_shouldNotGenerate() {
    StackGresPoolingConfig existingConfig = new StackGresPoolingConfigBuilder()
        .withNewMetadata()
        .withLabels(Map.of("other-label", "other-value"))
        .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
        .endMetadata()
        .build();
    when(context.getCoordinatorPoolingConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_generatedPoolingConfigHasCorrectNameAndNamespace() {
    when(context.getCoordinatorPoolingConfig()).thenReturn(Optional.empty());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPoolingConfig config = (StackGresPoolingConfig) resources.getFirst();
    assertEquals("pgbouncerconf", config.getMetadata().getName());
    assertEquals("stackgres", config.getMetadata().getNamespace());
  }

}
