/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.initialization.DefaultClusterPostgresConfigFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterDefaultPostgresConfigTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private DefaultClusterPostgresConfigFactory defaultClusterPostgresConfigFactory;

  @Mock
  private StackGresClusterContext context;

  private ClusterDefaultPostgresConfig clusterDefaultPostgresConfig;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    clusterDefaultPostgresConfig =
        new ClusterDefaultPostgresConfig(labelFactory, defaultClusterPostgresConfigFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);

    lenient().when(defaultClusterPostgresConfigFactory.buildResource(any()))
        .thenReturn(new StackGresPostgresConfigBuilder().withNewSpec().endSpec().build());
  }

  @Test
  void generateResource_whenPostgresConfigEmpty_shouldGenerateDefault() {
    when(context.getPostgresConfig()).thenReturn(Optional.empty());

    List<HasMetadata> resources =
        clusterDefaultPostgresConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals(cluster.getSpec().getConfigurations().getSgPostgresConfig(),
        config.getMetadata().getName());
  }

  @Test
  void generateResource_whenConfigExistsWithDefaultLabelsAndOwner_shouldGenerate() {
    StackGresPostgresConfig existingConfig = new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withLabels(Map.copyOf(labelFactory.defaultConfigLabels(cluster)))
        .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
        .endMetadata()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources =
        clusterDefaultPostgresConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals(cluster.getSpec().getConfigurations().getSgPostgresConfig(),
        config.getMetadata().getName());
  }

  @Test
  void generateResource_whenConfigExistsWithoutMatchingLabels_shouldNotGenerate() {
    StackGresPostgresConfig existingConfig = new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withLabels(Map.of("other-label", "other-value"))
        .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
        .endMetadata()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources =
        clusterDefaultPostgresConfig.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_generatedConfigHasCorrectNameAndNamespace() {
    when(context.getPostgresConfig()).thenReturn(Optional.empty());

    List<HasMetadata> resources =
        clusterDefaultPostgresConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals("postgresconf", config.getMetadata().getName());
    assertEquals("stackgres", config.getMetadata().getNamespace());
  }

}
