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
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.BackupConfigFinder;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ObjectStorageFinder;
import io.stackgres.common.resource.ProfileConfigFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BackupRequiredResourcesGeneratorTest {

  @InjectMock
  ClusterFinder clusterFinder;

  @InjectMock
  ProfileConfigFinder profileFinder;

  @InjectMock
  BackupConfigFinder backupConfigFinder;

  @InjectMock
  ObjectStorageFinder objectStorageFinder;

  @Inject
  BackupRequiredResourcesGenerator generator;

  private StackGresBackup backup;
  private StackGresBackupConfig backupConfig;
  private StackGresObjectStorage objectStorage;
  private StackGresCluster cluster;
  private StackGresProfile profile;

  @BeforeEach
  void setUp() {
    backup = Fixtures.backup().loadDefault().get();
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL.getLatest()
        .getLatestVersion());
    cluster.getMetadata().setNamespace(backup.getMetadata().getNamespace());
    cluster.getMetadata().setName(backup.getSpec().getSgCluster());
    profile = Fixtures.instanceProfile().loadSizeXs().get();
    backupConfig = Fixtures.backupConfig().loadDefault().get();
    backupConfig.getMetadata().setNamespace(backup.getMetadata().getNamespace());
    objectStorage = Fixtures.objectStorage().loadDefault().get();
  }

  @Test
  @DisplayName("Given a SGCluster with valid SGBackupConfig should not fail")
  void testValidClusterBackupConfigConfiguration() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getResourceProfile();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    when(backupConfigFinder.findByNameAndNamespace(backupConfigName, backupNamespace))
        .thenReturn(Optional.of(this.backupConfig));

    generator.getRequiredResources(backup);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(backupNamespace));
    verify(backupConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder).findByNameAndNamespace(eq(backupConfigName), eq(backupNamespace));
  }

  @Test
  void givenValidClusterWithBackupCopy_getRequiredResourcesShouldNotFail() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = "test." + backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getResourceProfile();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();

    backup.getSpec().setSgCluster(clusterName);

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    when(backupConfigFinder.findByNameAndNamespace(backupConfigName, backupNamespace))
        .thenReturn(Optional.of(this.backupConfig));

    generator.getRequiredResources(backup);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(
        eq(StackGresUtil.getNameFromRelativeId(clusterName)),
        eq(StackGresUtil.getNamespaceFromRelativeId(clusterName, backupNamespace)));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(
        eq(profileName),
        eq(StackGresUtil.getNamespaceFromRelativeId(clusterName, backupNamespace)));
    verify(backupConfigFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidCluster_getRequiredResourcesAllReturnedResourcesShouldHaveTheOwnerReference() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getResourceProfile();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

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
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(backupNamespace));
    verify(backupConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder).findByNameAndNamespace(eq(backupConfigName), eq(backupNamespace));
  }

  @DisplayName("Given a SGCluster with a valid SGObjectStorage should not fail")
  void testValidObjectStorageConfiguration() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = backup.getSpec().getSgCluster();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    clusterConfiguration.setBackupConfig(null);
    var backupConfiguration = new StackGresClusterBackupConfiguration();
    clusterConfiguration.setBackups(
        List.of(backupConfiguration));
    backupConfiguration.setObjectStorage(objectStorage.getMetadata().getName());

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(objectStorageFinder.findByNameAndNamespace(
        objectStorage.getMetadata().getName(), backupNamespace))
            .thenReturn(Optional.of(objectStorage));
    generator.getRequiredResources(backup);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(
        eq(StackGresUtil.getNameFromRelativeId(clusterName)),
        eq(StackGresUtil.getNamespaceFromRelativeId(clusterName, backupNamespace)));
    verify(backupConfigFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  @DisplayName("Given a backup with invalid SGCluster should fail")
  void testBackupInvalidSgCluster() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = backup.getSpec().getSgCluster();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());

    generator.getRequiredResources(backup);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(profileFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  @DisplayName("Given a Cluster with invalid backup config, generated resources should fail")
  void testSgClusterInvalidBackupConfig() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getResourceProfile();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    final String backupConfigName = clusterConfiguration.getBackupConfig();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    when(backupConfigFinder.findByNameAndNamespace(backupConfigName, backupNamespace))
        .thenReturn(Optional.empty());

    assertException("SGBackup " + backupNamespace + "." + backupName
        + " target SGCluster " + clusterName
        + " with a non existent SGBackupConfig " + backupConfigName);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(backupNamespace));
    verify(backupConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder).findByNameAndNamespace(eq(backupConfigName), eq(backupNamespace));
  }

  @Test
  @DisplayName("Given a SgCluster with an invalid SgGObjectStorage should fail")
  void testSgCLusterInvalidSgObjectStorage() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getResourceProfile();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    clusterConfiguration.setBackupConfig(null);
    var backupConfiguration = new StackGresClusterBackupConfiguration();
    clusterConfiguration.setBackups(
        List.of(backupConfiguration));
    backupConfiguration.setObjectStorage(objectStorage.getMetadata().getName());

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(objectStorageFinder.findByNameAndNamespace(
        objectStorage.getMetadata().getName(), backupNamespace)).thenReturn(Optional.empty());

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    assertException("SGBackup " + backupNamespace + "." + backupName
        + " target SGCluster " + clusterName
        + " with a non existent SGObjectStorage " + objectStorage.getMetadata().getName());

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(backupNamespace));
    verify(objectStorageFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(objectStorageFinder).findByNameAndNamespace(
        eq(objectStorage.getMetadata().getName()), eq(backupNamespace));
  }

  @Test
  @DisplayName("Given a SGCluster with no backup configuration should fail")
  void testSgClusterWithoutBackupConfiguration() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getResourceProfile();
    final StackGresClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = clusterSpec.getConfiguration();
    clusterConfiguration.setBackupConfig(null);

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    assertException("SGBackup " + backupNamespace + "." + backupName
        + " target SGCluster " + clusterName
        + " without a SGObjectStorage or SGBackupConfig");

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(backupNamespace));
    verify(objectStorageFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenABackupWithClusterWithoutProfile_getRequiredResourcesShouldFail() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getResourceProfile();
    final String backupConfigName = cluster.getSpec().getConfiguration().getBackupConfig();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(backupConfigFinder.findByNameAndNamespace(backupConfigName, backupNamespace))
        .thenReturn(Optional.of(this.backupConfig));

    assertException("SGBackup " + backupNamespace + "." + backupName
        + " target SGCluster " + clusterName
        + " with a non existent SGInstanceProfile " + profileName);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(backupNamespace));
    verify(objectStorageFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(backupConfigFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  private void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(backup));
    assertEquals(message, ex.getMessage());
  }

}
