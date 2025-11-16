/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static io.stackgres.operator.utils.ConciliationUtils.toNumericPostgresVersion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicationBuilder;
import io.stackgres.common.crd.sgcluster.StackGresReplicationInitializationMode;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorageBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceScanner;
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
class ClusterReplicationInitializationContextAppenderTest {

  private ClusterReplicationInitializationContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private CustomResourceScanner<StackGresBackup> backupScanner;

  @Mock
  private LabelFactoryForCluster labelFactory;

  private BackupEnvVarFactory backupEnvVarFactory = new BackupEnvVarFactory();

  private StackGresObjectStorage objectStorage;
  private Secret secret;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterReplicationInitializationContextAppender(
        secretFinder, backupEnvVarFactory, backupScanner, labelFactory);
    objectStorage = new StackGresObjectStorageBuilder()
        .withNewMetadata()
        .withName("objectstorage")
        .endMetadata()
        .withNewSpec()
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
        .endSpec()
        .build();
    secret = new SecretBuilder()
        .withData(Map.of(
            "accessKeyId", "test",
            "secretAccessKey", "test"))
        .build();
  }

  @Test
  void givenClusterWithReplicationInitializationFromBackup_shouldPass() {
    final StackGresBackup oldBackup = new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("old-backup")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(BackupStatus.COMPLETED.status())
        .withNewTiming()
        .withEnd(Instant.now().minusSeconds(20).toString())
        .endTiming()
        .endProcess()
        .withNewBackupInformation()
        .withPostgresVersion(toNumericPostgresVersion(cluster.getSpec().getPostgres().getVersion()))
        .endBackupInformation()
        .withBackupPath(cluster.getSpec().getConfigurations().getBackups().getFirst().getPath())
        .withNewSgBackupConfig()
        .withStorage(objectStorage.getSpec())
        .endSgBackupConfig()
        .endStatus()
        .build();
    final StackGresBackup latestBackup = new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("latest-backup")
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
        .withPostgresVersion(toNumericPostgresVersion(cluster.getSpec().getPostgres().getVersion()))
        .endBackupInformation()
        .withBackupPath(cluster.getSpec().getConfigurations().getBackups().getFirst().getPath())
        .withNewSgBackupConfig()
        .withStorage(objectStorage.getSpec())
        .endSgBackupConfig()
        .endStatus()
        .build();
    final StackGresBackup pendingBackup = new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("pending-backup")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(BackupStatus.PENDING.status())
        .endProcess()
        .endStatus()
        .build();
    final StackGresBackup failedBackup = new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("failed-backup")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(BackupStatus.FAILED.status())
        .endProcess()
        .endStatus()
        .build();
    when(backupScanner.getResources(any()))
        .thenReturn(List.of(
            oldBackup,
            latestBackup,
            pendingBackup,
            failedBackup));
    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(secret));

    contextAppender.appendContext(cluster, contextBuilder,
        Optional.of(objectStorage), cluster.getSpec().getPostgres().getVersion());

    verify(backupScanner, times(1)).getResources(any());
    verify(secretFinder, times(2)).findByNameAndNamespace(any(), any());
    verify(contextBuilder).replicationInitializationBackup(Optional.of(latestBackup));
    verify(contextBuilder).replicationInitializationBackupToCreate(Optional.empty());
    verify(contextBuilder).replicationInitializationSecrets(
        Map.of(
            objectStorage.getSpec().getS3Compatible().getAwsCredentials()
            .getSecretKeySelectors().getAccessKeyId().getName(),
            secret));
  }

  @Test
  void givenClusterWithReplicationInitializationFromNewlyCreatedBackupWaitingBackupCreated_shouldPass() {
    cluster.getSpec().setReplication(
        new StackGresClusterReplicationBuilder()
        .withNewInitialization()
        .withMode(StackGresReplicationInitializationMode.FROM_NEWLY_CREATED_BACKUP.toString())
        .endInitialization()
        .build());
    final StackGresBackup backupCreated = new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("backup-created")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(BackupStatus.PENDING.status())
        .endProcess()
        .withNewBackupInformation()
        .endBackupInformation()
        .endStatus()
        .build();
    final StackGresBackup failedBackup = new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("failed-backup")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(BackupStatus.FAILED.status())
        .endProcess()
        .endStatus()
        .build();
    when(backupScanner.getResources(any()))
        .thenReturn(List.of(
            backupCreated,
            failedBackup));
    when(backupScanner.getResourcesWithLabels(any(), any()))
        .thenReturn(List.of(
            backupCreated,
            failedBackup));

    contextAppender.appendContext(cluster, contextBuilder,
        Optional.of(objectStorage), cluster.getSpec().getPostgres().getVersion());

    verify(backupScanner, times(1)).getResources(any());
    verify(secretFinder, never()).findByNameAndNamespace(any(), any());
    verify(contextBuilder).replicationInitializationBackup(Optional.empty());
    verify(contextBuilder).replicationInitializationBackupToCreate(Optional.of(backupCreated));
    verify(contextBuilder).replicationInitializationSecrets(
        Map.of());
  }

  @Test
  void givenClusterWithReplicationInitializationFromNewlyCreatedBackup_shouldPass() {
    cluster.getSpec().setReplication(
        new StackGresClusterReplicationBuilder()
        .withNewInitialization()
        .withMode(StackGresReplicationInitializationMode.FROM_NEWLY_CREATED_BACKUP.toString())
        .endInitialization()
        .build());
    final StackGresBackup backupCreated = new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("backup-created")
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
        .withPostgresVersion(toNumericPostgresVersion(cluster.getSpec().getPostgres().getVersion()))
        .endBackupInformation()
        .withBackupPath(cluster.getSpec().getConfigurations().getBackups().getFirst().getPath())
        .withNewSgBackupConfig()
        .withStorage(objectStorage.getSpec())
        .endSgBackupConfig()
        .endStatus()
        .build();
    final StackGresBackup failedBackup = new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("failed-backup")
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(BackupStatus.FAILED.status())
        .endProcess()
        .endStatus()
        .build();
    when(backupScanner.getResources(any()))
        .thenReturn(List.of(
            backupCreated,
            failedBackup));
    when(backupScanner.getResourcesWithLabels(any(), any()))
        .thenReturn(List.of(
            backupCreated,
            failedBackup));
    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(secret));

    contextAppender.appendContext(cluster, contextBuilder,
        Optional.of(objectStorage), cluster.getSpec().getPostgres().getVersion());

    verify(backupScanner, times(1)).getResources(any());
    verify(secretFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(contextBuilder).replicationInitializationBackup(Optional.of(backupCreated));
    verify(contextBuilder).replicationInitializationBackupToCreate(Optional.of(backupCreated));
    verify(contextBuilder).replicationInitializationSecrets(
        Map.of(
            objectStorage.getSpec().getS3Compatible().getAwsCredentials()
            .getSecretKeySelectors().getAccessKeyId().getName(),
            secret));
  }

}
