/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.restore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
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
class RestoreConfigMapTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private StackGresClusterContext context;

  private RestoreConfigMap restoreConfigMap;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    restoreConfigMap = new RestoreConfigMap();
    restoreConfigMap.setLabelFactory(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getRestoreBackup()).thenReturn(Optional.empty());
    lenient().when(context.getPodDataPersistentVolumeNames()).thenReturn(Map.of());
  }

  @Test
  void buildVolumes_whenRestoreBackupEmpty_shouldContainErrorEntry() {
    when(context.getRestoreBackup()).thenReturn(Optional.empty());

    List<VolumePair> pairs = restoreConfigMap.buildVolumes(context).toList();

    assertEquals(1, pairs.size());
    VolumePair pair = pairs.get(0);
    assertTrue(pair.getSource().isPresent());
    ConfigMap configMap = (ConfigMap) pair.getSource().get();
    assertTrue(configMap.getData().containsKey("RESTORE_BACKUP_ERROR"));
    assertEquals("Can not restore from backup. Backup not found!",
        configMap.getData().get("RESTORE_BACKUP_ERROR"));
  }

  @Test
  void buildVolumes_whenBackupPresentAndCompleted_shouldGenerateConfigMap() {
    StackGresBackup backup = Fixtures.backup().loadDefault().get();
    backup.getStatus().getProcess().setStatus(BackupStatus.COMPLETED.status());
    backup.getStatus().setInternalName("test-internal-name");
    backup.getStatus().setBackupPath("sgbackups.stackgres.io/test/path");
    when(context.getRestoreBackup()).thenReturn(Optional.of(backup));

    List<VolumePair> pairs = restoreConfigMap.buildVolumes(context).toList();

    assertEquals(1, pairs.size());
    VolumePair pair = pairs.get(0);

    String expectedName =
        StackGresVolume.RESTORE_ENV.getResourceName(cluster.getMetadata().getName());
    assertEquals(StackGresVolume.RESTORE_ENV.getName(), pair.getVolume().getName());
    assertEquals(expectedName, pair.getVolume().getConfigMap().getName());

    assertTrue(pair.getSource().isPresent());
    ConfigMap configMap = (ConfigMap) pair.getSource().get();
    assertEquals(expectedName, configMap.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(),
        configMap.getMetadata().getNamespace());
    assertFalse(configMap.getMetadata().getLabels().isEmpty());

    assertNotNull(configMap.getData());
    assertTrue(configMap.getData().containsKey("RESTORE_BACKUP_NAME"));
    assertEquals("test-internal-name", configMap.getData().get("RESTORE_BACKUP_NAME"));
    assertTrue(configMap.getData().containsKey("RESTORE_VOLUME_SNAPSHOT"));
    assertTrue(configMap.getData().containsKey(StackGresUtil.MD5SUM_KEY));
    assertFalse(configMap.getData().containsKey("RESTORE_BACKUP_ERROR"));
  }

  @Test
  void buildVolumes_whenBackupPresentButNotCompleted_shouldContainErrorEntry() {
    StackGresBackup backup = Fixtures.backup().loadDefault().get();
    backup.getStatus().getProcess().setStatus(BackupStatus.RUNNING.status());
    when(context.getRestoreBackup()).thenReturn(Optional.of(backup));

    List<VolumePair> pairs = restoreConfigMap.buildVolumes(context).toList();

    assertEquals(1, pairs.size());
    ConfigMap configMap = (ConfigMap) pairs.get(0).getSource().orElseThrow();
    assertTrue(configMap.getData().containsKey("RESTORE_BACKUP_ERROR"));
    assertEquals("Backup is Running", configMap.getData().get("RESTORE_BACKUP_ERROR"));
  }

  @Test
  void buildVolumes_whenBackupPendingStatus_shouldContainErrorEntry() {
    StackGresBackup backup = Fixtures.backup().loadDefault().get();
    backup.getStatus().setProcess(new StackGresBackupProcess());
    when(context.getRestoreBackup()).thenReturn(Optional.of(backup));

    List<VolumePair> pairs = restoreConfigMap.buildVolumes(context).toList();

    ConfigMap configMap = (ConfigMap) pairs.get(0).getSource().orElseThrow();
    assertTrue(configMap.getData().containsKey("RESTORE_BACKUP_ERROR"));
    assertEquals("Backup is Pending", configMap.getData().get("RESTORE_BACKUP_ERROR"));
  }
}
