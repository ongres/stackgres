/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterBackupNamespacesContextAppenderTest {

  private ClusterBackupNamespacesContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private CustomResourceScanner<StackGresBackup> backupScanner;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterBackupNamespacesContextAppender(
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
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName("backup")
            .endMetadata()
            .withNewSpec()
            .withSgCluster(cluster.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName("another")
            .endMetadata()
            .withNewSpec()
            .withSgCluster("another-cluster")
            .endSpec()
            .build()));
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).clusterBackupNamespaces(Set.of());
  }

  @Test
  void givenClusterWithBackupsInOtherNamespaces_shouldPass() {
    when(backupScanner.getResources()).thenReturn(
        List.of(
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName("backup")
            .endMetadata()
            .withNewSpec()
            .withSgCluster(cluster.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
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
            .withSgCluster(cluster.getMetadata().getNamespace() + "." + cluster.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace("copied")
            .withName("other-copied")
            .endMetadata()
            .withNewSpec()
            .withSgCluster(cluster.getMetadata().getNamespace() + "." + cluster.getMetadata().getName())
            .endSpec()
            .build(),
            new StackGresBackupBuilder()
            .withNewMetadata()
            .withNamespace("other-copied")
            .withName("yet-another-copied")
            .endMetadata()
            .withNewSpec()
            .withSgCluster(cluster.getMetadata().getNamespace() + "." + cluster.getMetadata().getName())
            .endSpec()
            .build()));
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).clusterBackupNamespaces(Set.of("copied", "other-copied"));
  }

}
