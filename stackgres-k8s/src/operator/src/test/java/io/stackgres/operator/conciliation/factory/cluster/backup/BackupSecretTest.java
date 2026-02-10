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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.crd.storages.BackupStorageBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupSecretTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private BackupEnvVarFactory backupEnvVarFactory;

  @Mock
  private StackGresClusterContext context;

  private BackupSecret backupSecret;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    backupSecret = new BackupSecret();
    backupSecret.setLabelFactory(labelFactory);
    backupSecret.setBackupEnvVarFactory(backupEnvVarFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getBackupConfigurationResourceVersion())
        .thenReturn(Optional.empty());
    lenient().when(context.getBackupStorage()).thenReturn(Optional.empty());
    lenient().when(context.getBackupSecrets()).thenReturn(Map.of());
  }

  @Test
  void buildVolumes_shouldReturnVolumePairWithCorrectSecretName() {
    List<VolumePair> pairs = backupSecret.buildVolumes(context).toList();

    assertEquals(1, pairs.size());
    VolumePair pair = pairs.get(0);

    String expectedName =
        StackGresVolume.BACKUP_CREDENTIALS.getResourceName(cluster.getMetadata().getName());
    assertEquals(StackGresVolume.BACKUP_CREDENTIALS.getName(), pair.getVolume().getName());
    assertEquals(expectedName, pair.getVolume().getSecret().getSecretName());

    assertTrue(pair.getSource().isPresent());
    assertEquals(expectedName, pair.getSource().get().getMetadata().getName());
  }

  @Test
  void buildVolumes_whenBackupStoragePresent_shouldIncludeCredentials() {
    BackupStorage storage = new BackupStorageBuilder().build();
    when(context.getBackupStorage()).thenReturn(Optional.of(storage));
    Map<String, String> secretEnvVars = Map.of("AWS_ACCESS_KEY_ID", "myKey",
        "AWS_SECRET_ACCESS_KEY", "mySecret");
    when(backupEnvVarFactory.getSecretEnvVar(
        eq(cluster.getMetadata().getNamespace()), eq(storage), anyMap()))
        .thenReturn(secretEnvVars);

    List<VolumePair> pairs = backupSecret.buildVolumes(context).toList();
    Secret secret = (Secret) pairs.get(0).getSource().orElseThrow();

    Map<String, String> decodedData = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(decodedData.containsKey("AWS_ACCESS_KEY_ID"));
    assertEquals("myKey", decodedData.get("AWS_ACCESS_KEY_ID"));
    assertTrue(decodedData.containsKey("AWS_SECRET_ACCESS_KEY"));
    assertEquals("mySecret", decodedData.get("AWS_SECRET_ACCESS_KEY"));
  }

  @Test
  void buildVolumes_whenBackupConfigResourceVersion_shouldIncludeIt() {
    when(context.getBackupConfigurationResourceVersion())
        .thenReturn(Optional.of("12345"));

    List<VolumePair> pairs = backupSecret.buildVolumes(context).toList();
    Secret secret = (Secret) pairs.get(0).getSource().orElseThrow();

    Map<String, String> decodedData = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(decodedData.containsKey(BACKUP_CONFIG_RESOURCE_VERSION_KEY));
    assertEquals("12345", decodedData.get(BACKUP_CONFIG_RESOURCE_VERSION_KEY));
  }

  @Test
  void buildVolumes_whenNoBackupStorage_shouldHaveMinimalData() {
    List<VolumePair> pairs = backupSecret.buildVolumes(context).toList();
    Secret secret = (Secret) pairs.get(0).getSource().orElseThrow();

    assertNotNull(secret.getData());
    Map<String, String> decodedData = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(decodedData.containsKey(StackGresUtil.MD5SUM_KEY));
    assertTrue(decodedData.containsKey(StackGresUtil.MD5SUM_2_KEY));
    assertEquals(2, decodedData.size());
  }

  @Test
  void buildVolumes_secretHasCorrectLabelsAndNamespace() {
    List<VolumePair> pairs = backupSecret.buildVolumes(context).toList();
    Secret secret = (Secret) pairs.get(0).getSource().orElseThrow();

    assertEquals(cluster.getMetadata().getNamespace(),
        secret.getMetadata().getNamespace());
    assertFalse(secret.getMetadata().getLabels().isEmpty());
    assertEquals("Opaque", secret.getType());
  }
}
