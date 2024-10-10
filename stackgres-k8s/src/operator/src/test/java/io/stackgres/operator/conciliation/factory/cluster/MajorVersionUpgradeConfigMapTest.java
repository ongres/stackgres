/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MajorVersionUpgradeConfigMapTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(StackGresContextMock.CONTEXT, new ClusterLabelMapper());

  @Mock
  private StackGresClusterContext context;

  private MajorVersionUpgradeConfigMap majorVersionUpgradeConfigMap;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    majorVersionUpgradeConfigMap = new MajorVersionUpgradeConfigMap(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getSource()).thenReturn(cluster);
  }

  private void setMajorVersionUpgradeStatus() {
    if (cluster.getStatus() == null) {
      cluster.setStatus(new StackGresClusterStatus());
    }
    cluster.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
    cluster.getStatus().getDbOps().setMajorVersionUpgrade(
        new StackGresClusterDbOpsMajorVersionUpgradeStatus());
  }

  private void setPostgresConfigs() {
    StackGresPostgresConfig targetPostgresConfig = new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresqlConf(Map.of("max_connections", "200", "shared_buffers", "256MB"))
        .endSpec()
        .build();
    lenient().when(context.getPostgresConfig()).thenReturn(Optional.of(targetPostgresConfig));
  }

  @Test
  void buildSource_shouldGenerateConfigMapWithPostgresConf() {
    setPostgresConfigs();

    HasMetadata source = majorVersionUpgradeConfigMap.buildSource(context);

    assertNotNull(source);
    assertTrue(source instanceof ConfigMap);
    ConfigMap configMap = (ConfigMap) source;
    assertNotNull(configMap.getData());
    assertTrue(configMap.getData().containsKey("postgresql.conf"));
    String targetPgConf = configMap.getData().get("postgresql.conf");
    assertTrue(targetPgConf.contains("max_connections"));
    assertTrue(targetPgConf.contains("200"));
    assertTrue(targetPgConf.contains("shared_buffers"));
  }

  @Test
  void buildSource_whenNoPostgresConfig_shouldFail() {
    when(context.getPostgresConfig()).thenReturn(Optional.empty());

    var ex = assertThrows(RuntimeException.class,
        () -> majorVersionUpgradeConfigMap.buildSource(context));

    assertEquals("SGPostgresConfig not found", ex.getMessage());
  }

  @Test
  void buildSource_shouldHaveCorrectNamespaceAndName() {
    setPostgresConfigs();

    HasMetadata source = majorVersionUpgradeConfigMap.buildSource(context);

    ConfigMap configMap = (ConfigMap) source;
    assertEquals(cluster.getMetadata().getNamespace(), configMap.getMetadata().getNamespace());
    String expectedName = StackGresVolume.POSTGRES_CONFIG
        .getResourceName(cluster.getMetadata().getName());
    assertEquals(expectedName, configMap.getMetadata().getName());
  }

  @Test
  void buildSource_shouldHaveLabels() {
    setPostgresConfigs();

    HasMetadata source = majorVersionUpgradeConfigMap.buildSource(context);

    ConfigMap configMap = (ConfigMap) source;
    assertNotNull(configMap.getMetadata().getLabels());
    assertFalse(configMap.getMetadata().getLabels().isEmpty());
  }

  @Test
  void buildVolumes_whenMajorVersionUpgradeInProgress_shouldReturnSingleVolumePair() {
    setMajorVersionUpgradeStatus();
    setPostgresConfigs();

    List<VolumePair> volumePairs = majorVersionUpgradeConfigMap.buildVolumes(context).toList();

    assertEquals(1, volumePairs.size());
    VolumePair volumePair = volumePairs.getFirst();
    assertEquals(StackGresVolume.POSTGRES_CONFIG.getName(),
        volumePair.getVolume().getName());
    assertTrue(volumePair.getSource().isPresent());
  }

  @Test
  void buildVolumes_whenNoMajorVersionUpgradeInProgress_shouldReturnNoVolumePair() {
    if (cluster.getStatus() != null) {
      cluster.getStatus().setDbOps(null);
    }

    List<VolumePair> volumePairs = majorVersionUpgradeConfigMap.buildVolumes(context).toList();

    assertTrue(volumePairs.isEmpty());
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
    setPostgresConfigs();

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
