/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatusBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupClusterContextAppenderTest {

  private BackupClusterContextAppender contextAppender;

  private StackGresBackup backup;

  private StackGresCluster cluster;

  @Spy
  private StackGresBackupContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  @Mock
  private BackupClusterInstanceProfileContextAppender backupClusterInstanceProfileContextAppender;

  @Mock
  private BackupClusterObjectStorageContextAppender backupClusterObjectStorageContextAppender;

  @BeforeEach
  void setUp() {
    backup = Fixtures.backup().loadDefault().get();
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new BackupClusterContextAppender(
        clusterFinder,
        backupClusterInstanceProfileContextAppender,
        backupClusterObjectStorageContextAppender);
  }

  @Test
  void givenBackupWithCluster_shouldPass() {
    when(clusterFinder.findByNameAndNamespace(
        backup.getSpec().getSgCluster(),
        backup.getMetadata().getNamespace()))
        .thenReturn(Optional.of(cluster));
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).foundCluster(Optional.of(cluster));
    verify(backupClusterInstanceProfileContextAppender).appendContext(cluster, contextBuilder);
    verify(backupClusterObjectStorageContextAppender).appendContext(cluster, contextBuilder);
  }

  @Test
  void givenBackupCopyWithCluster_shouldPass() {
    backup.getSpec().setSgCluster("test.test");
    when(clusterFinder.findByNameAndNamespace(
        "test",
        "test"))
        .thenReturn(Optional.of(cluster));
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).foundCluster(Optional.of(cluster));
    verify(backupClusterInstanceProfileContextAppender).appendContext(cluster, contextBuilder);
    verify(backupClusterObjectStorageContextAppender).appendContext(cluster, contextBuilder);
  }

  @Test
  void givenBackupWithoutCluster_shouldFail() {
    when(clusterFinder.findByNameAndNamespace(
        backup.getSpec().getSgCluster(),
        backup.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(backup, contextBuilder));
    assertEquals("SGCluster backup-with-default-storage was not found", ex.getMessage());
  }

  @Test
  void givenBackupCopyWithoutCluster_shouldPass() {
    backup.getSpec().setSgCluster("test.test");
    when(clusterFinder.findByNameAndNamespace("test", "test"))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).foundCluster(Optional.empty());
    verify(backupClusterInstanceProfileContextAppender, Mockito.never()).appendContext(Mockito.any(), Mockito.any());
    verify(backupClusterObjectStorageContextAppender, Mockito.never()).appendContext(Mockito.any(), Mockito.any());
  }

  @Test
  void givenCompletedBackupWithoutCluster_shouldPass() {
    backup.setStatus(
        new StackGresBackupStatusBuilder()
        .withNewProcess()
        .withStatus(BackupStatus.COMPLETED.status())
        .endProcess()
        .build());
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).foundCluster(Optional.empty());
    verify(clusterFinder, Mockito.never()).findByNameAndNamespace(
        backup.getSpec().getSgCluster(),
        backup.getMetadata().getNamespace());
    verify(backupClusterInstanceProfileContextAppender, Mockito.never()).appendContext(Mockito.any(), Mockito.any());
    verify(backupClusterObjectStorageContextAppender, Mockito.never()).appendContext(Mockito.any(), Mockito.any());
  }

}
