/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterBackupNamespacesContextAppenderTest {

  private ShardedClusterBackupNamespacesContextAppender contextAppender;

  private StackGresShardedCluster cluster;

  @Spy
  private StackGresShardedClusterContext.Builder contextBuilder;

  @Mock
  private CustomResourceScanner<StackGresShardedBackup> backupScanner;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    contextAppender = new ShardedClusterBackupNamespacesContextAppender(
        backupScanner);
  }

  @Test
  void givenClusterWithoutBackups_shouldPass() {
    when(backupScanner.getResources()).thenReturn(List.of());
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).clusterBackupNamespaces(Set.of());
  }

  @Test
  void givenClusterWithoutBackupsInOtherNamespaces_shouldPass() {
    when(backupScanner.getResources()).thenReturn(
        List.of(
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName("backup")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster(cluster.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName("another")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster("another-cluster")
            .endSpec()
            .build()));
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).clusterBackupNamespaces(Set.of());
  }

  @Test
  void givenClusterWithBackupsInOtherNamespaces_shouldPass() {
    when(backupScanner.getResources()).thenReturn(
        List.of(
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName("backup")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster(cluster.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName("another")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster("another-cluster")
            .endSpec()
            .build(),
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace("copied")
            .withName("copied")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster(cluster.getMetadata().getNamespace() + "." + cluster.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace("copied")
            .withName("other-copied")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster(cluster.getMetadata().getNamespace() + "." + cluster.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace("other-copied")
            .withName("yet-another-copied")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster(cluster.getMetadata().getNamespace() + "." + cluster.getMetadata().getName())
            .endSpec()
            .build()));
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).clusterBackupNamespaces(Set.of("copied", "other-copied"));
  }

}
