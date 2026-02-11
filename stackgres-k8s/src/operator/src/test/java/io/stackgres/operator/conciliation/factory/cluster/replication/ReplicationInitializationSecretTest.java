/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.replication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
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
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReplicationInitializationSecretTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private BackupEnvVarFactory backupEnvVarFactory;

  @Mock
  private StackGresClusterContext context;

  private ReplicationInitializationSecret replicationInitializationSecret;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    replicationInitializationSecret = new ReplicationInitializationSecret();
    replicationInitializationSecret.setLabelFactory(labelFactory);
    replicationInitializationSecret.setEnvVarFactory(backupEnvVarFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getReplicationInitializationBackup()).thenReturn(Optional.empty());
    lenient().when(context.getReplicationInitializationSecrets()).thenReturn(Map.of());
  }

  @Test
  void buildVolumes_whenReplicationBackupEmpty_shouldReturnSecretWithMinimalData() {
    when(context.getReplicationInitializationBackup()).thenReturn(Optional.empty());

    List<VolumePair> pairs = replicationInitializationSecret.buildVolumes(context).toList();

    assertEquals(1, pairs.size());
    VolumePair pair = pairs.get(0);
    assertTrue(pair.getSource().isPresent());
    Secret secret = (Secret) pair.getSource().get();
    Map<String, String> decodedData = ResourceUtil.decodeSecret(secret.getData());
    assertNotNull(decodedData);
    assertTrue(decodedData.containsKey(StackGresUtil.MD5SUM_KEY));
    assertTrue(decodedData.containsKey(StackGresUtil.MD5SUM_2_KEY));
    assertEquals(2, decodedData.size());
  }

  @Test
  void buildVolumes_whenBackupPresent_shouldGenerateSecretWithCredentials() {
    StackGresBackup backup = Fixtures.backup().loadDefault().get();
    backup.getStatus().getProcess().setStatus(BackupStatus.COMPLETED.status());
    when(context.getReplicationInitializationBackup()).thenReturn(Optional.of(backup));
    when(backupEnvVarFactory.getSecretEnvVar(
        eq(backup.getMetadata().getNamespace()),
        eq(backup.getStatus().getSgBackupConfig()),
        anyMap()))
        .thenReturn(Map.of("AWS_ACCESS_KEY_ID", "replKey"));

    List<VolumePair> pairs = replicationInitializationSecret.buildVolumes(context).toList();

    assertEquals(1, pairs.size());
    VolumePair pair = pairs.get(0);

    String expectedName =
        StackGresVolume.REPLICATION_INITIALIZATION_CREDENTIALS.getResourceName(
            cluster.getMetadata().getName());
    assertEquals(StackGresVolume.REPLICATION_INITIALIZATION_CREDENTIALS.getName(),
        pair.getVolume().getName());
    assertEquals(expectedName, pair.getVolume().getSecret().getSecretName());

    assertTrue(pair.getSource().isPresent());
    Secret secret = (Secret) pair.getSource().get();
    assertEquals(expectedName, secret.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(),
        secret.getMetadata().getNamespace());
    assertFalse(secret.getMetadata().getLabels().isEmpty());
    assertEquals("Opaque", secret.getType());

    Map<String, String> decodedData = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(decodedData.containsKey("AWS_ACCESS_KEY_ID"));
    assertEquals("replKey", decodedData.get("AWS_ACCESS_KEY_ID"));
    assertTrue(decodedData.containsKey(StackGresUtil.MD5SUM_KEY));
  }
}
