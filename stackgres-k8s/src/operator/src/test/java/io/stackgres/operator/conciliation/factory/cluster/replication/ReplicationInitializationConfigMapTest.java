/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.replication;

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
import io.stackgres.common.ClusterPath;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
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
class ReplicationInitializationConfigMapTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private StackGresClusterContext context;

  private ReplicationInitializationConfigMap replicationInitializationConfigMap;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    replicationInitializationConfigMap = new ReplicationInitializationConfigMap();
    replicationInitializationConfigMap.setLabelFactory(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getReplicationInitializationBackup()).thenReturn(Optional.empty());
    lenient().when(context.getReplicateConfiguration()).thenReturn(Optional.empty());
    lenient().when(context.getPodDataPersistentVolumeNames()).thenReturn(Map.of());
  }

  @Test
  void buildVolumes_whenReplicationBackupEmpty_shouldReturnConfigMapWithPathEntriesOnly() {
    when(context.getReplicationInitializationBackup()).thenReturn(Optional.empty());

    List<VolumePair> pairs = replicationInitializationConfigMap.buildVolumes(context).toList();

    assertEquals(1, pairs.size());
    VolumePair pair = pairs.get(0);
    assertTrue(pair.getSource().isPresent());
    ConfigMap configMap = (ConfigMap) pair.getSource().get();
    assertNotNull(configMap.getData());
    assertTrue(configMap.getData().containsKey(
        ClusterPath.PG_REPLICATION_BASE_PATH.name()));
    assertTrue(configMap.getData().containsKey(
        ClusterPath.PG_REPLICATION_INITIALIZATION_FAILED_BACKUP_PATH.name()));
    assertTrue(configMap.getData().containsKey(StackGresUtil.MD5SUM_KEY));
    assertFalse(configMap.getData().containsKey(
        PatroniUtil.REPLICATION_INITIALIZATION_BACKUP));
  }

  @Test
  void buildVolumes_whenBackupPresent_shouldGenerateConfigMapWithBackupEntries() {
    StackGresBackup backup = Fixtures.backup().loadDefault().get();
    backup.getStatus().getProcess().setStatus(BackupStatus.COMPLETED.status());
    backup.getStatus().setInternalName("repl-internal-name");
    backup.getStatus().setBackupPath("sgbackups.stackgres.io/repl/path");
    when(context.getReplicationInitializationBackup()).thenReturn(Optional.of(backup));

    List<VolumePair> pairs = replicationInitializationConfigMap.buildVolumes(context).toList();

    assertEquals(1, pairs.size());
    VolumePair pair = pairs.get(0);

    String expectedName =
        StackGresVolume.REPLICATION_INITIALIZATION_ENV.getResourceName(
            cluster.getMetadata().getName());
    assertEquals(StackGresVolume.REPLICATION_INITIALIZATION_ENV.getName(),
        pair.getVolume().getName());
    assertEquals(expectedName, pair.getVolume().getConfigMap().getName());

    assertTrue(pair.getSource().isPresent());
    ConfigMap configMap = (ConfigMap) pair.getSource().get();
    assertEquals(expectedName, configMap.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(),
        configMap.getMetadata().getNamespace());
    assertFalse(configMap.getMetadata().getLabels().isEmpty());

    assertNotNull(configMap.getData());
    assertTrue(configMap.getData().containsKey(
        PatroniUtil.REPLICATION_INITIALIZATION_BACKUP));
    assertEquals(backup.getMetadata().getName(),
        configMap.getData().get(PatroniUtil.REPLICATION_INITIALIZATION_BACKUP));
    assertTrue(configMap.getData().containsKey("REPLICATION_INITIALIZATION_BACKUP_NAME"));
    assertEquals("repl-internal-name",
        configMap.getData().get("REPLICATION_INITIALIZATION_BACKUP_NAME"));
    assertTrue(configMap.getData().containsKey("REPLICATION_INITIALIZATION_VOLUME_SNAPSHOT"));
    assertTrue(configMap.getData().containsKey(
        ClusterPath.PG_REPLICATION_BASE_PATH.name()));
    assertTrue(configMap.getData().containsKey(StackGresUtil.MD5SUM_KEY));
  }
}
