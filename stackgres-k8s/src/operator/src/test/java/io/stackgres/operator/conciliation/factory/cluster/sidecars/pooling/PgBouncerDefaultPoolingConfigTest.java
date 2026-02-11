/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

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
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.initialization.DefaultPoolingConfigFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgBouncerDefaultPoolingConfigTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private DefaultPoolingConfigFactory defaultPoolingConfigFactory;

  @Mock
  private StackGresClusterContext context;

  private PgBouncerDefaultPoolingConfig pgBouncerDefaultPoolingConfig;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    pgBouncerDefaultPoolingConfig =
        new PgBouncerDefaultPoolingConfig(labelFactory, defaultPoolingConfigFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);

    lenient().when(defaultPoolingConfigFactory.buildResource(any()))
        .thenReturn(new StackGresPoolingConfigBuilder().withNewSpec().endSpec().build());
  }

  @Test
  void generateResource_whenPoolingEnabledAndNoConfigExists_shouldGenerateDefault() {
    when(context.getPoolingConfig()).thenReturn(Optional.empty());

    List<HasMetadata> resources =
        pgBouncerDefaultPoolingConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPoolingConfig config = (StackGresPoolingConfig) resources.getFirst();
    assertEquals(cluster.getSpec().getConfigurations().getSgPoolingConfig(),
        config.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(),
        config.getMetadata().getNamespace());
  }

  @Test
  void generateResource_whenPoolingDisabled_shouldNotGenerate() {
    cluster.getSpec().getPods().setDisableConnectionPooling(true);

    List<HasMetadata> resources =
        pgBouncerDefaultPoolingConfig.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenConfigExistsWithMatchingLabelsAndOwner_shouldGenerate() {
    StackGresPoolingConfig existingConfig = new StackGresPoolingConfigBuilder()
        .withNewMetadata()
        .withLabels(Map.copyOf(labelFactory.defaultConfigLabels(cluster)))
        .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
        .endMetadata()
        .build();
    when(context.getPoolingConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources =
        pgBouncerDefaultPoolingConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPoolingConfig config = (StackGresPoolingConfig) resources.getFirst();
    assertEquals(cluster.getSpec().getConfigurations().getSgPoolingConfig(),
        config.getMetadata().getName());
  }

  @Test
  void generateResource_whenConfigExistsWithoutMatchingLabels_shouldNotGenerate() {
    StackGresPoolingConfig existingConfig = new StackGresPoolingConfigBuilder()
        .withNewMetadata()
        .withLabels(Map.of("other-label", "other-value"))
        .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
        .endMetadata()
        .build();
    when(context.getPoolingConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources =
        pgBouncerDefaultPoolingConfig.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_generatedConfigHasCorrectNameAndNamespace() {
    when(context.getPoolingConfig()).thenReturn(Optional.empty());

    List<HasMetadata> resources =
        pgBouncerDefaultPoolingConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPoolingConfig config = (StackGresPoolingConfig) resources.getFirst();
    assertEquals(cluster.getSpec().getConfigurations().getSgPoolingConfig(),
        config.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(),
        config.getMetadata().getNamespace());
  }

}
