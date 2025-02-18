/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup.context;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupClusterBackupNamespacesContextAppenderTest {

  private BackupClusterBackupNamespacesContextAppender contextAppender;

  private StackGresBackup backup;

  @Spy
  private StackGresBackupContext.Builder contextBuilder;

  @Mock
  private CustomResourceScanner<StackGresBackup> backupScanner;

  @BeforeEach
  void setUp() {
    backup = Fixtures.backup().loadDefault().get();
    contextAppender = new BackupClusterBackupNamespacesContextAppender(
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
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace(backup.getMetadata().getNamespace())
            .withName("backup")
            .endMetadata()
            .withNewSpec()
            .withSgCluster(backup.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace(backup.getMetadata().getNamespace())
            .withName("another")
            .endMetadata()
            .withNewSpec()
            .withSgCluster("another-cluster")
            .endSpec()
            .build()));
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).clusterBackupNamespaces(Set.of());
  }

  @Test
  void givenClusterWithBackupsInOtherNamespaces_shouldPass() {
    when(backupScanner.getResources()).thenReturn(
        List.of(
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace(backup.getMetadata().getNamespace())
            .withName("backup")
            .endMetadata()
            .withNewSpec()
            .withSgCluster(backup.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace(backup.getMetadata().getNamespace())
            .withName("another")
            .endMetadata()
            .withNewSpec()
            .withSgCluster("another-cluster")
            .endSpec()
            .build(),
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace("copied")
            .withName("copied")
            .endMetadata()
            .withNewSpec()
            .withSgCluster(backup.getMetadata().getNamespace() + "." + backup.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace("copied")
            .withName("other-copied")
            .endMetadata()
            .withNewSpec()
            .withSgCluster(backup.getMetadata().getNamespace() + "." + backup.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace("other-copied")
            .withName("yet-another-copied")
            .endMetadata()
            .withNewSpec()
            .withSgCluster(backup.getMetadata().getNamespace() + "." + backup.getMetadata().getName())
            .endSpec()
            .build()));
    contextAppender.appendContext(backup, contextBuilder);
    verify(contextBuilder).clusterBackupNamespaces(Set.of("copied", "other-copied"));
  }

}
