/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.resource.BackupConfigFinder;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BackupRequiredResourcesGeneratorTest {

  @InjectMock
  ClusterFinder clusterFinder;

  @InjectMock
  BackupConfigFinder backupConfigFinder;

  @InjectMock
  BackupRequiredResourceDecorator decorator;

  @Inject
  BackupRequiredResourcesGenerator generator;

  private StackGresBackup backup;
  private StackGresBackupConfig backupConfig;
  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    backup = JsonUtil
        .readFromJson("backup/default.json", StackGresBackup.class);
    cluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL.getLatest()
        .findLatestVersion());
    cluster.getMetadata().setNamespace(backup.getMetadata().getNamespace());
    cluster.getMetadata().setName(backup.getSpec().getSgCluster());
    backupConfig = JsonUtil.readFromJson("backup_config/default.json", StackGresBackupConfig.class);
    backupConfig.getMetadata().setNamespace(backup.getMetadata().getNamespace());
  }

  @Test
  void givenValidCluster_getRequiredResourcesShouldNotFail() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = backup.getSpec().getSgCluster();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(backupConfigFinder.findByNameAndNamespace(backupConfigName, backupNamespace))
        .thenReturn(Optional.of(this.backupConfig));

    generator.getRequiredResources(backup);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(backupConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder).findByNameAndNamespace(eq(backupConfigName), eq(backupNamespace));
  }

  @Test
  void givenValidClusterWithBackupCopy_getRequiredResourcesShouldNotFail() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = "test." + backup.getSpec().getSgCluster();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();

    backup.getSpec().setSgCluster(clusterName);

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(backupConfigFinder.findByNameAndNamespace(backupConfigName, backupNamespace))
        .thenReturn(Optional.of(this.backupConfig));

    generator.getRequiredResources(backup);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(
        eq(StackGresUtil.getNameFromRelativeId(clusterName)),
        eq(StackGresUtil.getNamespaceFromRelativeId(clusterName, backupNamespace)));
    verify(backupConfigFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidCluster_getRequiredResourcesAllReturnedResourcesShouldHaveTheOwnerReference() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = backup.getSpec().getSgCluster();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(backupConfigFinder.findByNameAndNamespace(backupConfigName, backupNamespace))
        .thenReturn(Optional.of(this.backupConfig));

    List<HasMetadata> resources = generator.getRequiredResources(backup);

    resources.forEach(resource -> {
      assertNotNull(resource.getMetadata().getOwnerReferences(),
          "Resource " + resource.getMetadata().getName() + " doesn't owner references");
      if (resource.getMetadata().getOwnerReferences().size() == 0) {
        fail("Resource " + resource.getMetadata().getName() + " doesn't have any owner");
      }
      assertTrue(resource.getMetadata().getOwnerReferences().stream()
          .anyMatch(ownerReference -> ownerReference.getApiVersion()
              .equals(HasMetadata.getApiVersion(StackGresBackup.class))
              && ownerReference.getKind().equals(HasMetadata.getKind(StackGresBackup.class))
              && ownerReference.getName().equals(backup.getMetadata().getName())
              && ownerReference.getUid().equals(backup.getMetadata().getUid())
              && Optional.ofNullable(ownerReference.getBlockOwnerDeletion()).orElse(Boolean.FALSE)
                  .equals(Boolean.FALSE)));
    });

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(backupConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder).findByNameAndNamespace(eq(backupConfigName), eq(backupNamespace));
  }

  @Test
  void givenABackupInvalidCluster_getRequiredResourcesShouldFail() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgCluster();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());

    assertException("SGBackup " + backupNamespace + "/" + backupName
        + " target a non existent SGCluster " + backupNamespace + "." + clusterName);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(backupConfigFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenABackupWithClusterWithInvalidBackupConfig_getRequiredResourcesShouldFail() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgCluster();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(backupConfigFinder.findByNameAndNamespace(backupConfigName, backupNamespace))
        .thenReturn(Optional.empty());

    assertException("SGBackup " + backupNamespace + "/" + backupName
        + " target SGCluster " + clusterName
        + " with a non existent SGBackupConfig " + backupConfigName);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(backupConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder).findByNameAndNamespace(eq(backupConfigName), eq(backupNamespace));
  }

  @Test
  void givenABackupWithClusterWithoutBackupConfig_getRequiredResourcesShouldFail() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgCluster();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    clusterConfiguration.setBackupConfig(null);

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    assertException("SGBackup " + backupNamespace + "/" + backupName
        + " target SGCluster " + clusterName
        + " without a SGBackupConfig");

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(backupConfigFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  private void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(backup));
    assertEquals(message, ex.getMessage());
  }

}
