/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.initialization.DefaultClusterPostgresConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MajorVersionUpgradeConfigMapTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private DefaultClusterPostgresConfigFactory defaultPostgresConfigFactory;

  @Mock
  private StackGresClusterContext context;

  private MajorVersionUpgradeConfigMap majorVersionUpgradeConfigMap;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    majorVersionUpgradeConfigMap =
        new MajorVersionUpgradeConfigMap(labelFactory, defaultPostgresConfigFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getCluster()).thenReturn(cluster);
  }

  @Test
  void buildSource_shouldGenerateConfigMapWithPostgresConf() {
    StackGresPostgresConfig postgresConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(Map.of("max_connections", "200", "shared_buffers", "256MB"))
        .endSpec()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(postgresConfig));

    HasMetadata source = majorVersionUpgradeConfigMap.buildSource(context);

    assertNotNull(source);
    assertTrue(source instanceof ConfigMap);
    ConfigMap configMap = (ConfigMap) source;
    assertNotNull(configMap.getData());
    assertTrue(configMap.getData().containsKey("postgresql.conf"));
    String pgConf = configMap.getData().get("postgresql.conf");
    assertTrue(pgConf.contains("max_connections"));
    assertTrue(pgConf.contains("shared_buffers"));
  }

  @Test
  void buildSource_whenCustomPostgresConfigAvailable_shouldUseCustomConfig() {
    Map<String, String> customConf = Map.of(
        "max_connections", "500",
        "work_mem", "64MB");
    StackGresPostgresConfig postgresConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(customConf)
        .endSpec()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(postgresConfig));

    HasMetadata source = majorVersionUpgradeConfigMap.buildSource(context);

    ConfigMap configMap = (ConfigMap) source;
    String pgConf = configMap.getData().get("postgresql.conf");
    assertTrue(pgConf.contains("max_connections"));
    assertTrue(pgConf.contains("500"));
    assertTrue(pgConf.contains("work_mem"));
    assertTrue(pgConf.contains("64MB"));
  }

  @Test
  void buildSource_whenNoPostgresConfig_shouldFallBackToDefault() {
    StackGresPostgresConfig defaultConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(Map.of("max_connections", "100"))
        .endSpec()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.empty());
    when(defaultPostgresConfigFactory.buildResource(any())).thenReturn(defaultConfig);

    HasMetadata source = majorVersionUpgradeConfigMap.buildSource(context);

    ConfigMap configMap = (ConfigMap) source;
    String pgConf = configMap.getData().get("postgresql.conf");
    assertTrue(pgConf.contains("max_connections"));
    assertTrue(pgConf.contains("100"));
  }

  @Test
  void buildSource_shouldHaveCorrectNamespaceAndName() {
    StackGresPostgresConfig postgresConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(Map.of("max_connections", "100"))
        .endSpec()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(postgresConfig));

    HasMetadata source = majorVersionUpgradeConfigMap.buildSource(context);

    ConfigMap configMap = (ConfigMap) source;
    assertEquals(cluster.getMetadata().getNamespace(), configMap.getMetadata().getNamespace());
    String expectedName = StackGresVolume.POSTGRES_CONFIG
        .getResourceName(cluster.getMetadata().getName());
    assertEquals(expectedName, configMap.getMetadata().getName());
  }

  @Test
  void buildSource_shouldHaveLabels() {
    StackGresPostgresConfig postgresConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(Map.of("max_connections", "100"))
        .endSpec()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(postgresConfig));

    HasMetadata source = majorVersionUpgradeConfigMap.buildSource(context);

    ConfigMap configMap = (ConfigMap) source;
    assertNotNull(configMap.getMetadata().getLabels());
    assertFalse(configMap.getMetadata().getLabels().isEmpty());
  }

  @Test
  void buildVolumes_shouldReturnSingleVolumePair() {
    StackGresPostgresConfig postgresConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(Map.of("max_connections", "100"))
        .endSpec()
        .build();
    when(context.getPostgresConfig()).thenReturn(Optional.of(postgresConfig));

    List<VolumePair> volumePairs = majorVersionUpgradeConfigMap.buildVolumes(context).toList();

    assertEquals(1, volumePairs.size());
    VolumePair volumePair = volumePairs.getFirst();
    assertEquals(StackGresVolume.POSTGRES_CONFIG.getName(),
        volumePair.getVolume().getName());
    assertTrue(volumePair.getSource().isPresent());
  }

  @Test
  void buildVolume_shouldHaveCorrectConfigMapReference() {
    var volume = majorVersionUpgradeConfigMap.buildVolume(context);

    assertEquals(StackGresVolume.POSTGRES_CONFIG.getName(), volume.getName());
    assertNotNull(volume.getConfigMap());
    String expectedName = StackGresVolume.POSTGRES_CONFIG
        .getResourceName(cluster.getMetadata().getName());
    assertEquals(expectedName, volume.getConfigMap().getName());
    assertEquals(0444, volume.getConfigMap().getDefaultMode());
  }

  @Test
  void buildSource_shouldContainMd5Sum() {
    StackGresPostgresConfig postgresConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(Map.of("max_connections", "100"))
        .endSpec()
        .build();
    lenient().when(context.getPostgresConfig()).thenReturn(Optional.of(postgresConfig));

    HasMetadata source = majorVersionUpgradeConfigMap.buildSource(context);

    ConfigMap configMap = (ConfigMap) source;
    assertTrue(configMap.getData().keySet().stream()
        .anyMatch(key -> key.startsWith(StackGresUtil.MD5SUM_KEY)),
        "ConfigMap should contain MD5SUM key");
    assertTrue(configMap.getData().keySet().stream()
        .anyMatch(key -> key.startsWith(StackGresUtil.MD5SUM_2_KEY)),
        "ConfigMap should contain MD5SUM_2 key");
  }

}
