/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupBuilder;
import io.stackgres.common.crd.sgshardedcluster.ShardedClusterStatusCondition;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterInitialDataBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatusBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterRestoreBackupContextAppenderTest {

  private ShardedClusterRestoreBackupContextAppender contextAppender;

  private StackGresShardedCluster cluster;

  @Spy
  private StackGresShardedClusterContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresShardedBackup> backupFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getSpec().setInitialData(
        new StackGresShardedClusterInitialDataBuilder()
        .withNewRestore()
        .withNewFromBackup()
        .withName("backup")
        .endFromBackup()
        .endRestore()
        .build());
    contextAppender = new ShardedClusterRestoreBackupContextAppender(backupFinder);
  }

  @Test
  void givenClusterWithoutBackup_shouldPass() {
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.empty());
    contextAppender.appendContext(
        cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion());
    assertNull(cluster.getStatus());
  }

  @Test
  void givenBootstrappedCluster_shouldPass() {
    cluster.setStatus(
        new StackGresShardedClusterStatusBuilder()
        .addToConditions(ShardedClusterStatusCondition.SHARDED_CLUSTER_BOOTSTRAPPED.getCondition())
        .build());
    contextAppender.appendContext(
        cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion());
    verify(backupFinder, Mockito.never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithBackup_shouldPass() {
    final Optional<StackGresShardedBackup> backup = Optional.of(
        new StackGresShardedBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgShardedCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(ShardedBackupStatus.COMPLETED.status())
        .withNewTiming()
        .withEnd(Instant.now().minusSeconds(10).toString())
        .endTiming()
        .endProcess()
        .withNewBackupInformation()
        .withPostgresVersion(cluster.getSpec().getPostgres().getVersion())
        .endBackupInformation()
        .withSgBackups(List.of("1", "2", "3"))
        .endStatus()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    contextAppender.appendContext(
        cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion());
    assertNotNull(cluster.getStatus());
    assertNotNull(cluster.getStatus().getSgBackups());
    assertEquals(List.of("1", "2", "3"), cluster.getStatus().getSgBackups());
  }

  @Test
  void givenClusterWithUninitializedBackup_shouldFail() {
    final Optional<StackGresShardedBackup> backup = Optional.of(
        new StackGresShardedBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgShardedCluster(cluster.getMetadata().getName())
        .endSpec()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(
            cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion()));
    assertEquals("Cannot restore from SGShardedBackup backup because it's not Completed", ex.getMessage());
  }

  @Test
  void givenClusterWithPendingBackup_shouldFail() {
    final Optional<StackGresShardedBackup> backup = Optional.of(
        new StackGresShardedBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgShardedCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(ShardedBackupStatus.PENDING.status())
        .endProcess()
        .endStatus()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(
            cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion()));
    assertEquals("Cannot restore from SGShardedBackup backup because it's not Completed", ex.getMessage());
  }

  @Test
  void givenClusterWithFailedBackup_shouldFail() {
    final Optional<StackGresShardedBackup> backup = Optional.of(
        new StackGresShardedBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgShardedCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(ShardedBackupStatus.FAILED.status())
        .endProcess()
        .endStatus()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(
            cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion()));
    assertEquals("Cannot restore from SGShardedBackup backup because it's not Completed", ex.getMessage());
  }

  @Test
  void givenClusterWithSmallerBackupSize_shouldFail() {
    final Optional<StackGresShardedBackup> backup = Optional.of(
        new StackGresShardedBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgShardedCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(ShardedBackupStatus.COMPLETED.status())
        .withNewTiming()
        .withEnd(Instant.now().minusSeconds(10).toString())
        .endTiming()
        .endProcess()
        .withNewBackupInformation()
        .withPostgresVersion(cluster.getSpec().getPostgres().getVersion())
        .endBackupInformation()
        .withSgBackups(List.of("1", "2"))
        .endStatus()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(
            cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion()));
    assertEquals("In SGShardedBackup backup sgBackups must be an array of"
        + " size 3 (the coordinator plus the number of shards) but was 2", ex.getMessage());
  }

  @Test
  void givenClusterWithLargerBackupSize_shouldFail() {
    final Optional<StackGresShardedBackup> backup = Optional.of(
        new StackGresShardedBackupBuilder()
        .withNewMetadata()
        .withName("backup")
        .endMetadata()
        .withNewSpec()
        .withSgShardedCluster(cluster.getMetadata().getName())
        .endSpec()
        .withNewStatus()
        .withNewProcess()
        .withStatus(ShardedBackupStatus.COMPLETED.status())
        .withNewTiming()
        .withEnd(Instant.now().minusSeconds(10).toString())
        .endTiming()
        .endProcess()
        .withNewBackupInformation()
        .withPostgresVersion(cluster.getSpec().getPostgres().getVersion())
        .endBackupInformation()
        .withSgBackups(List.of("1", "2", "3", "4"))
        .endStatus()
        .build());
    when(backupFinder.findByNameAndNamespace(any(), any())).thenReturn(backup);
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(
            cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion()));
    assertEquals("In SGShardedBackup backup sgBackups must be an array of"
        + " size 3 (the coordinator plus the number of shards) but was 4", ex.getMessage());
  }

}
