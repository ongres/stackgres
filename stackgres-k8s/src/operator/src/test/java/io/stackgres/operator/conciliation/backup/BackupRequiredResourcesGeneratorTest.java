/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfigurationBuilder;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ObjectStorageFinder;
import io.stackgres.common.resource.ProfileFinder;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BackupRequiredResourcesGeneratorTest {

  @InjectMock
  ClusterFinder clusterFinder;

  @InjectMock
  ProfileFinder profileFinder;

  @InjectMock
  ObjectStorageFinder objectStorageFinder;

  @Inject
  BackupRequiredResourcesGenerator generator;

  private StackGresBackup backup;
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
    profile = Fixtures.instanceProfile().loadSizeS().get();
    objectStorage = Fixtures.objectStorage().loadDefault().get();
  }

  @Test
  @DisplayName("Given a SGCluster with valid SGObjectStorage should not fail")
  void testValidClusterObjectStorage() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getSgInstanceProfile();
    final String objectStorageName = cluster.getSpec()
        .getConfigurations()
        .getBackups()
        .get(0)
        .getSgObjectStorage();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    when(objectStorageFinder.findByNameAndNamespace(
        objectStorageName, backupNamespace)).thenReturn(Optional.of(objectStorage));

    generator.getRequiredResources(backup);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(backupNamespace));
    verify(objectStorageFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(objectStorageFinder).findByNameAndNamespace(eq(objectStorageName), eq(backupNamespace));
  }

  @Test
  void givenValidClusterWithBackupCopy_getRequiredResourcesShouldNotFail() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = "test." + backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getSgInstanceProfile();
    final String objectStorageName = cluster.getSpec()
        .getConfigurations()
        .getBackups()
        .get(0)
        .getSgObjectStorage();

    backup.getSpec().setSgCluster(clusterName);

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    when(objectStorageFinder.findByNameAndNamespace(
        objectStorageName, backupNamespace)).thenReturn(Optional.empty());

    generator.getRequiredResources(backup);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(
        eq(StackGresUtil.getNameFromRelativeId(clusterName)),
        eq(StackGresUtil.getNamespaceFromRelativeId(clusterName, backupNamespace)));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(
        eq(profileName),
        eq(StackGresUtil.getNamespaceFromRelativeId(clusterName, backupNamespace)));
    verify(objectStorageFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @DisplayName("Given a SGCluster with a valid SGObjectStorage should not fail")
  void testValidObjectStorageConfiguration() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = backup.getSpec().getSgCluster();
    cluster.getSpec()
        .getConfigurations()
        .setBackups(List.of(new StackGresClusterBackupConfigurationBuilder()
            .withSgObjectStorage(objectStorage.getMetadata().getName())
            .build()));

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
    verify(objectStorageFinder, times(0)).findByNameAndNamespace(any(), any());
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
    verify(objectStorageFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  @DisplayName("Given a SgCluster with an invalid SgGObjectStorage should fail")
  void testSgCLusterInvalidSgObjectStorage() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getSgInstanceProfile();
    cluster.getSpec()
        .getConfigurations()
        .setBackups(
            List.of(new StackGresClusterBackupConfigurationBuilder()
                .withSgObjectStorage(objectStorage.getMetadata().getName())
                .build()));

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
  void testSgClusterWithoutObjectStorage() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getSgInstanceProfile();
    cluster.getSpec().getConfigurations().setBackups(null);

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    assertException("SGBackup " + backupNamespace + "." + backupName
        + " target SGCluster " + clusterName
        + " without an SGObjectStorage");

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(backupNamespace));
    verify(objectStorageFinder, times(0)).findByNameAndNamespace(any(), any());
    verify(objectStorageFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenABackupWithClusterWithoutProfile_getRequiredResourcesShouldFail() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getSgInstanceProfile();
    final String objectStorageName = cluster.getSpec()
        .getConfigurations()
        .getBackups()
        .get(0)
        .getSgObjectStorage();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(objectStorageFinder.findByNameAndNamespace(
        objectStorageName, backupNamespace)).thenReturn(Optional.of(objectStorage));

    assertException("SGBackup " + backupNamespace + "." + backupName
        + " target SGCluster " + clusterName
        + " with a non existent SGInstanceProfile " + profileName);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(backupNamespace));
    verify(objectStorageFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  private void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(backup));
    assertEquals(message, ex.getMessage());
  }

}
