/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatusBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedBackupClusterContextAppenderTest {

  private ShardedBackupClusterContextAppender contextAppender;

  private StackGresShardedBackup backup;

  private StackGresShardedCluster cluster;

  @Spy
  private StackGresShardedBackupContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  @Mock
  private ShardedBackupClusterInstanceProfileContextAppender backupClusterInstanceProfileContextAppender;

  @Mock
  private ShardedBackupClusterObjectStorageContextAppender backupClusterObjectStorageContextAppender;

  @BeforeEach
  void setUp() {
    backup = Fixtures.shardedBackup().loadDefault().get();
    cluster = Fixtures.shardedCluster().loadDefault().get();
    contextAppender = new ShardedBackupClusterContextAppender(
        clusterFinder,
        backupClusterInstanceProfileContextAppender,
        backupClusterObjectStorageContextAppender);
  }

  @Test
  void givenBackupWithCluster_shouldPass() {
    when(clusterFinder.findByNameAndNamespace(
        backup.getSpec().getSgShardedCluster(),
        backup.getMetadata().getNamespace()))
        .thenReturn(Optional.of(cluster));
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).foundShardedCluster(Optional.of(cluster));
    verify(backupClusterInstanceProfileContextAppender).appendContext(cluster, contextBuilder);
    verify(backupClusterObjectStorageContextAppender).appendContext(cluster, contextBuilder);
  }

  @Test
  void givenBackupWithoutCluster_shouldFail() {
    when(clusterFinder.findByNameAndNamespace(
        backup.getSpec().getSgShardedCluster(),
        backup.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(backup, contextBuilder));
    assertEquals("SGShardedCluster backup-with-default-storage was not found", ex.getMessage());
  }

  @Test
  void givenCompletedBackupWithoutCluster_shouldPass() {
    backup.setStatus(
        new StackGresShardedBackupStatusBuilder()
        .withNewProcess()
        .withStatus(ShardedBackupStatus.COMPLETED.status())
        .endProcess()
        .build());
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).foundShardedCluster(Optional.empty());
    verify(clusterFinder, Mockito.never()).findByNameAndNamespace(
        backup.getSpec().getSgShardedCluster(),
        backup.getMetadata().getNamespace());
    verify(backupClusterInstanceProfileContextAppender, Mockito.never()).appendContext(Mockito.any(), Mockito.any());
    verify(backupClusterObjectStorageContextAppender, Mockito.never()).appendContext(Mockito.any(), Mockito.any());
  }

  @Test
  void givenBackupInAnotherNamespaceWithoutCluster_shouldPass() {
    backup.getSpec().setSgShardedCluster("test.test");
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).foundShardedCluster(Optional.empty());
    verify(backupClusterInstanceProfileContextAppender, Mockito.never()).appendContext(Mockito.any(), Mockito.any());
    verify(backupClusterObjectStorageContextAppender, Mockito.never()).appendContext(Mockito.any(), Mockito.any());
  }

}
