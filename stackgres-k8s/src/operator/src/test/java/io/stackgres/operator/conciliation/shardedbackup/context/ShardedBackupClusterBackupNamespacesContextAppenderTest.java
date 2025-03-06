/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup.context;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedBackupClusterBackupNamespacesContextAppenderTest {

  private ShardedBackupClusterBackupNamespacesContextAppender contextAppender;

  private StackGresShardedBackup backup;

  @Spy
  private StackGresShardedBackupContext.Builder contextBuilder;

  @Mock
  private CustomResourceScanner<StackGresShardedBackup> backupScanner;

  @BeforeEach
  void setUp() {
    backup = Fixtures.shardedBackup().loadDefault().get();
    contextAppender = new ShardedBackupClusterBackupNamespacesContextAppender(
        backupScanner);
  }

  @Test
  void givenClusterWithoutBackups_shouldPass() {
    when(backupScanner.getResources()).thenReturn(List.of());
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).clusterBackupNamespaces(Set.of());
  }

  @Test
  void givenClusterWithoutBackupsInOtherNamespaces_shouldPass() {
    when(backupScanner.getResources()).thenReturn(
        List.of(
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace(backup.getMetadata().getNamespace())
            .withName("backup")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster(backup.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace(backup.getMetadata().getNamespace())
            .withName("another")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster("another-cluster")
            .endSpec()
            .build()));
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).clusterBackupNamespaces(Set.of());
  }

  @Test
  void givenClusterWithBackupsInOtherNamespaces_shouldPass() {
    when(backupScanner.getResources()).thenReturn(
        List.of(
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace(backup.getMetadata().getNamespace())
            .withName("backup")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster(backup.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace(backup.getMetadata().getNamespace())
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
            .withSgShardedCluster(backup.getMetadata().getNamespace() + "." + backup.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace("copied")
            .withName("other-copied")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster(backup.getMetadata().getNamespace() + "." + backup.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresShardedBackupBuilder()
            .withNewMetadata()
            .withNamespace("other-copied")
            .withName("yet-another-copied")
            .endMetadata()
            .withNewSpec()
            .withSgShardedCluster(backup.getMetadata().getNamespace() + "." + backup.getMetadata().getName())
            .endSpec()
            .build()));
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).clusterBackupNamespaces(Set.of("copied", "other-copied"));
  }

}
