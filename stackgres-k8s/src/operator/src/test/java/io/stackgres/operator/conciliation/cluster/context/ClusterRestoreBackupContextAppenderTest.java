/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterRestoreBackupContextAppenderTest {

  private ClusterRestoreBackupContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private CustomResourceFinder<StackGresBackup> backupFinder;

  private BackupEnvVarFactory backupEnvVarFactory = new BackupEnvVarFactory();

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterRestoreBackupContextAppender(
        secretFinder, backupFinder, backupEnvVarFactory);
  }

  @Test
  void givenClusterWithoutBackup_shouldPass() {
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.empty());
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).restoreBackup(Optional.empty());
    verify(contextBuilder).restoreSecrets(Map.of());
  }

  @Test
  void givenClusterWithBackup_shouldPass() {
    final Optional<StackGresBackup> backup = Optional.of(
        new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(BackupStatus.COMPLETED.status())
        .withNewTiming()
        .withEnd(Instant.now().minusSeconds(10).toString())
        .endTiming()
        .endProcess()
        .withNewBackupInformation()
        .withPostgresVersion(cluster.getSpec().getPostgres().getVersion())
        .endBackupInformation()
        .withBackupPath(cluster.getSpec().getConfigurations().getBackups().getFirst().getPath())
        .withNewSgBackupConfig()
        .withNewStorage()
        .withType("s3Compatible")
        .withNewS3Compatible()
        .withBucket("test")
        .withNewAwsCredentials()
        .withNewSecretKeySelectors()
        .withNewAccessKeyId()
        .withName("test")
        .withKey("accessKeyId")
        .endAccessKeyId()
        .withNewSecretAccessKey()
        .withName("test")
        .withKey("secretAccessKey")
        .endSecretAccessKey()
        .endSecretKeySelectors()
        .endAwsCredentials()
        .endS3Compatible()
        .endStorage()
        .endSgBackupConfig()
        .endStatus()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    final Secret secret =
        new SecretBuilder()
        .withData(Map.of(
            "accessKeyId", "test",
            "secretAccessKey", "test"))
        .build();
    when(secretFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(secret));
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).restoreBackup(backup);
    verify(contextBuilder).restoreSecrets(Map.of("test", secret));
  }

  @Test
  void givenClusterWithUninitializedBackup_shouldFail() {
    final Optional<StackGresBackup> backup = Optional.of(
        new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Cannot restore from SGBackup backup because it's not Completed", ex.getMessage());
  }

  @Test
  void givenClusterWithPendingBackup_shouldFail() {
    final Optional<StackGresBackup> backup = Optional.of(
        new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(BackupStatus.PENDING.status())
        .endProcess()
        .endStatus()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Cannot restore from SGBackup backup because it's not Completed", ex.getMessage());
  }

  @Test
  void givenClusterWithFailedBackup_shouldFail() {
    final Optional<StackGresBackup> backup = Optional.of(
        new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(BackupStatus.FAILED.status())
        .endProcess()
        .endStatus()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Cannot restore from SGBackup backup because it's not Completed", ex.getMessage());
  }

  @Test
  void givenClusterWithBackupWithoutSecret_shouldFail() {
    final Optional<StackGresBackup> backup = Optional.of(
        new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(BackupStatus.COMPLETED.status())
        .withNewTiming()
        .withEnd(Instant.now().minusSeconds(10).toString())
        .endTiming()
        .endProcess()
        .withNewBackupInformation()
        .withPostgresVersion(cluster.getSpec().getPostgres().getVersion())
        .endBackupInformation()
        .withBackupPath(cluster.getSpec().getConfigurations().getBackups().getFirst().getPath())
        .withNewSgBackupConfig()
        .withNewStorage()
        .withType("s3Compatible")
        .withNewS3Compatible()
        .withBucket("test")
        .withNewAwsCredentials()
        .withNewSecretKeySelectors()
        .withNewAccessKeyId()
        .withName("test")
        .withKey("accessKeyId")
        .endAccessKeyId()
        .withNewSecretAccessKey()
        .withName("test")
        .withKey("secretAccessKey")
        .endSecretAccessKey()
        .endSecretKeySelectors()
        .endAwsCredentials()
        .endS3Compatible()
        .endStorage()
        .endSgBackupConfig()
        .endStatus()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    when(secretFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Secret test not found for SGBackup backup", ex.getMessage());
  }

  @Test
  void givenClusterWithBackupWithoutKey_shouldFail() {
    final Optional<StackGresBackup> backup = Optional.of(
        new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(BackupStatus.COMPLETED.status())
        .withNewTiming()
        .withEnd(Instant.now().minusSeconds(10).toString())
        .endTiming()
        .endProcess()
        .withNewBackupInformation()
        .withPostgresVersion(cluster.getSpec().getPostgres().getVersion())
        .endBackupInformation()
        .withBackupPath(cluster.getSpec().getConfigurations().getBackups().getFirst().getPath())
        .withNewSgBackupConfig()
        .withNewStorage()
        .withType("s3Compatible")
        .withNewS3Compatible()
        .withBucket("test")
        .withNewAwsCredentials()
        .withNewSecretKeySelectors()
        .withNewAccessKeyId()
        .withName("test")
        .withKey("accessKeyId")
        .endAccessKeyId()
        .withNewSecretAccessKey()
        .withName("test")
        .withKey("secretAccessKey")
        .endSecretAccessKey()
        .endSecretKeySelectors()
        .endAwsCredentials()
        .endS3Compatible()
        .endStorage()
        .endSgBackupConfig()
        .endStatus()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    final Secret secret =
        new SecretBuilder()
        .withData(Map.of(
            "secretAccessKey", "test"))
        .build();
    when(secretFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.of(secret));
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Key accessKeyId not found in Secret test for SGBackup backup", ex.getMessage());
  }

}
