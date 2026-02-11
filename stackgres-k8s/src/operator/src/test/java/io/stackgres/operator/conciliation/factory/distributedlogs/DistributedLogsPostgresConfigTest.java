/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsPostgresConfigTest {

  private final LabelFactoryForDistributedLogs labelFactory =
      new DistributedLogsLabelFactory(new DistributedLogsLabelMapper());

  @Mock
  private DefaultDistributedLogsPostgresConfigFactory defaultPostgresConfigFactory;

  @Mock
  private StackGresDistributedLogsContext context;

  private DistributedLogsPostgresConfig distributedLogsPostgresConfig;

  private StackGresDistributedLogs distributedLogs;

  @BeforeEach
  void setUp() {
    distributedLogsPostgresConfig =
        new DistributedLogsPostgresConfig(labelFactory, defaultPostgresConfigFactory);
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    when(context.getSource()).thenReturn(distributedLogs);
  }

  @Test
  void generateResource_shouldIncludeTimescaledbInSharedPreloadLibraries() {
    StackGresPostgresConfig existingConfig = new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withName("postgresconf")
        .endMetadata()
        .withNewSpec()
        .withPostgresVersion("17")
        .withPostgresqlConf(Map.of(
            "shared_preload_libraries", "pg_stat_statements"))
        .endSpec()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources =
        distributedLogsPostgresConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertNotNull(config.getSpec().getPostgresqlConf());
    String sharedPreloadLibraries =
        config.getSpec().getPostgresqlConf().get("shared_preload_libraries");
    assertNotNull(sharedPreloadLibraries);
    assertTrue(sharedPreloadLibraries.contains("timescaledb"),
        "Expected shared_preload_libraries to contain timescaledb but was: "
        + sharedPreloadLibraries);
  }

  @Test
  void generateResource_shouldSetTelemetryLevelToOff() {
    StackGresPostgresConfig existingConfig = new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withName("postgresconf")
        .endMetadata()
        .withNewSpec()
        .withPostgresVersion("17")
        .withPostgresqlConf(Map.of())
        .endSpec()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources =
        distributedLogsPostgresConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals("off",
        config.getSpec().getPostgresqlConf().get("timescaledb.telemetry_level"));
  }

  @Test
  void generateResource_whenPostgresConfigEmpty_shouldUseFallbackFromFactory() {
    StackGresPostgresConfig defaultConfig = new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withName("default-config")
        .endMetadata()
        .withNewSpec()
        .withPostgresVersion("17")
        .withPostgresqlConf(Map.of())
        .endSpec()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.empty());
    when(defaultPostgresConfigFactory.buildResource(any())).thenReturn(defaultConfig);

    List<HasMetadata> resources =
        distributedLogsPostgresConfig.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals(DistributedLogsPostgresConfig.configName(distributedLogs),
        config.getMetadata().getName());
    assertEquals("timescaledb",
        config.getSpec().getPostgresqlConf().get("shared_preload_libraries"));
  }

  @Test
  void generateResource_shouldHaveCorrectConfigName() {
    StackGresPostgresConfig existingConfig = new StackGresPostgresConfigBuilder()
        .withNewMetadata()
        .withName("postgresconf")
        .endMetadata()
        .withNewSpec()
        .withPostgresVersion("17")
        .withPostgresqlConf(Map.of())
        .endSpec()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(existingConfig));

    List<HasMetadata> resources =
        distributedLogsPostgresConfig.generateResource(context).toList();

    StackGresPostgresConfig config = (StackGresPostgresConfig) resources.getFirst();
    assertEquals("distributedlogs-logs", config.getMetadata().getName());
    assertEquals("distributed-logs", config.getMetadata().getNamespace());
  }

}
