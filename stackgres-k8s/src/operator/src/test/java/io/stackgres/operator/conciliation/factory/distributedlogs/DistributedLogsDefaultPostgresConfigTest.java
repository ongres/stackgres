/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.DistributedLogsLabelFactory;
import io.stackgres.common.labels.DistributedLogsLabelMapper;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.initialization.DefaultDistributedLogsPostgresConfigFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsDefaultPostgresConfigTest {

  private final LabelFactoryForDistributedLogs labelFactory =
      new DistributedLogsLabelFactory(new DistributedLogsLabelMapper());

  @Mock
  private DefaultDistributedLogsPostgresConfigFactory defaultDistributedLogsPostgresConfigFactory;

  @Mock
  private StackGresDistributedLogsContext context;

  private DistributedLogsDefaultPostgresConfig distributedLogsDefaultPostgresConfig;

  private StackGresDistributedLogs distributedLogs;

  @BeforeEach
  void setUp() {
    distributedLogsDefaultPostgresConfig =
        new DistributedLogsDefaultPostgresConfig(
            labelFactory, defaultDistributedLogsPostgresConfigFactory);
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    when(context.getSource()).thenReturn(distributedLogs);

    lenient().when(defaultDistributedLogsPostgresConfigFactory.buildResource(any()))
        .thenReturn(new StackGresPostgresConfigBuilder().withNewSpec().endSpec().build());
  }

  @Test
  void generateResource_whenPostgresConfigEmpty_shouldGenerateDefault() {
    when(context.getPostgresConfig()).thenReturn(Optional.empty());

    List<HasMetadata> resources =
        distributedLogsDefaultPostgresConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals(distributedLogs.getSpec().getConfigurations().getSgPostgresConfig(),
        config.getMetadata().getName());
  }

  @Test
  void generateResource_whenConfigExistsWithDefaultLabelsAndOwner_shouldGenerate() {
    StackGresPostgresConfig existingConfig = new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withLabels(Map.copyOf(labelFactory.defaultConfigLabels(distributedLogs)))
        .withOwnerReferences(
            List.of(ResourceUtil.getControllerOwnerReference(distributedLogs)))
        .endMetadata()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources =
        distributedLogsDefaultPostgresConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals(distributedLogs.getSpec().getConfigurations().getSgPostgresConfig(),
        config.getMetadata().getName());
  }

  @Test
  void generateResource_whenConfigExistsWithoutMatchingLabels_shouldNotGenerate() {
    StackGresPostgresConfig existingConfig = new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withLabels(Map.of("other-label", "other-value"))
        .withOwnerReferences(
            List.of(ResourceUtil.getControllerOwnerReference(distributedLogs)))
        .endMetadata()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources =
        distributedLogsDefaultPostgresConfig.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_generatedConfigHasCorrectNameAndNamespace() {
    when(context.getPostgresConfig()).thenReturn(Optional.empty());

    List<HasMetadata> resources =
        distributedLogsDefaultPostgresConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals("postgresconf", config.getMetadata().getName());
    assertEquals("distributed-logs", config.getMetadata().getNamespace());
  }

}
