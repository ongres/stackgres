/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import static io.stackgres.operator.conciliation.factory.cluster.backup.BackupConfigMap.BACKUP_CONFIG_RESOURCE_VERSION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
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
class BackupConfigMapTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private StackGresClusterContext context;

  private BackupConfigMap backupConfigMap;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    backupConfigMap = new BackupConfigMap();
    backupConfigMap.setLabelFactory(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getBackupConfigurationResourceVersion())
        .thenReturn(Optional.empty());
    lenient().when(context.getBackupStorage()).thenReturn(Optional.empty());
    lenient().when(context.getBackupConfiguration()).thenReturn(Optional.empty());
  }

  @Test
  void buildVolumes_shouldReturnVolumePairWithCorrectConfigMapName() {
    List<VolumePair> pairs = backupConfigMap.buildVolumes(context).toList();

    assertEquals(1, pairs.size());
    VolumePair pair = pairs.get(0);

    String expectedName =
        StackGresVolume.BACKUP_ENV.getResourceName(cluster.getMetadata().getName());
    assertEquals(StackGresVolume.BACKUP_ENV.getName(), pair.getVolume().getName());
    assertEquals(expectedName, pair.getVolume().getConfigMap().getName());

    assertTrue(pair.getSource().isPresent());
    assertEquals(expectedName, pair.getSource().get().getMetadata().getName());
  }

  @Test
  void buildVolumes_whenBackupConfigResourceVersion_shouldIncludeIt() {
    when(context.getBackupConfigurationResourceVersion())
        .thenReturn(Optional.of("67890"));

    List<VolumePair> pairs = backupConfigMap.buildVolumes(context).toList();
    ConfigMap configMap = (ConfigMap) pairs.get(0).getSource().orElseThrow();

    assertTrue(configMap.getData().containsKey(BACKUP_CONFIG_RESOURCE_VERSION_KEY));
    assertEquals("67890", configMap.getData().get(BACKUP_CONFIG_RESOURCE_VERSION_KEY));
  }

  @Test
  void buildVolumes_whenNoBackupConfigured_shouldHaveMinimalData() {
    List<VolumePair> pairs = backupConfigMap.buildVolumes(context).toList();
    ConfigMap configMap = (ConfigMap) pairs.get(0).getSource().orElseThrow();

    assertNotNull(configMap.getData());
    assertTrue(configMap.getData().containsKey(StackGresUtil.MD5SUM_KEY));
    assertTrue(configMap.getData().containsKey(StackGresUtil.MD5SUM_2_KEY));
  }

  @Test
  void buildVolumes_configMapHasCorrectLabels() {
    List<VolumePair> pairs = backupConfigMap.buildVolumes(context).toList();
    ConfigMap configMap = (ConfigMap) pairs.get(0).getSource().orElseThrow();

    assertEquals(cluster.getMetadata().getNamespace(),
        configMap.getMetadata().getNamespace());
    assertFalse(configMap.getMetadata().getLabels().isEmpty());
  }
}
