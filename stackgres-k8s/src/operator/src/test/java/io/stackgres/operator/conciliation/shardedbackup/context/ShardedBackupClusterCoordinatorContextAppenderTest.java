/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup.context;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatusBuilder;
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
class ShardedBackupClusterCoordinatorContextAppenderTest {

  private ShardedBackupClusterCoordinatorContextAppender contextAppender;

  private StackGresShardedBackup backup;

  private StackGresCluster cluster;

  @Spy
  private StackGresShardedBackupContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  @BeforeEach
  void setUp() {
    backup = Fixtures.shardedBackup().loadDefault().get();
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ShardedBackupClusterCoordinatorContextAppender(
        clusterFinder);
  }

  @Test
  void givenBackupWithCluster_shouldPass() {
    when(clusterFinder.findByNameAndNamespace(
        getCoordinatorClusterName(backup.getSpec().getSgShardedCluster()),
        backup.getMetadata().getNamespace()))
        .thenReturn(Optional.of(cluster));
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).foundCoordinator(Optional.of(cluster));
  }

  @Test
  void givenBackupWithoutCluster_shouldFail() {
    when(clusterFinder.findByNameAndNamespace(
        getCoordinatorClusterName(backup.getSpec().getSgShardedCluster()),
        backup.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(backup, contextBuilder));
    assertEquals("SGCluster backup-with-default-storage-coord was not found", ex.getMessage());
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
    verify(contextBuilder).foundCoordinator(Optional.empty());
    verify(clusterFinder, Mockito.never()).findByNameAndNamespace(
        getCoordinatorClusterName(backup.getSpec().getSgShardedCluster()),
        backup.getMetadata().getNamespace());
  }

  @Test
  void givenBackupInAnotherNamespaceWithoutCluster_shouldPass() {
    backup.getSpec().setSgShardedCluster("test.test");
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).foundCoordinator(Optional.empty());
  }

}
