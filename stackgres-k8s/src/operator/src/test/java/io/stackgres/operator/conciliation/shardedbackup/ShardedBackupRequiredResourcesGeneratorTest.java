/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfigurationBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ObjectStorageFinder;
import io.stackgres.common.resource.ProfileConfigFinder;
import io.stackgres.common.resource.ShardedClusterFinder;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShardedBackupRequiredResourcesGeneratorTest {

  @InjectMock
  ShardedClusterFinder clusterFinder;

  @InjectMock
  ProfileConfigFinder profileFinder;

  @InjectMock
  ObjectStorageFinder objectStorageFinder;

  @Inject
  ShardedBackupRequiredResourcesGenerator generator;

  private StackGresShardedBackup backup;
  private StackGresObjectStorage objectStorage;
  private StackGresShardedCluster cluster;
  private StackGresProfile profile;

  @BeforeEach
  void setUp() {
    backup = Fixtures.shardedBackup().loadDefault().get();
    profile = Fixtures.instanceProfile().loadSizeS().get();
    objectStorage = Fixtures.objectStorage().loadDefault().get();
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL.getLatest()
        .getLatestVersion());
    cluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
    cluster.getSpec().getConfigurations().setBackups(List.of(
        new StackGresShardedClusterBackupConfigurationBuilder()
        .withSgObjectStorage(objectStorage.getMetadata().getName())
        .build()));
    cluster.getMetadata().setNamespace(backup.getMetadata().getNamespace());
    cluster.getMetadata().setName(backup.getSpec().getSgShardedCluster());
  }

  @Test
  void givenValidClusterWithBackupCopy_getRequiredResourcesShouldNotFail() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = "test." + backup.getSpec().getSgShardedCluster();
    final String profileName = cluster.getSpec().getCoordinator().getSgInstanceProfile();

    backup.getSpec().setSgShardedCluster(clusterName);

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    generator.getRequiredResources(backup);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(
        eq(StackGresUtil.getNameFromRelativeId(clusterName)),
        eq(StackGresUtil.getNamespaceFromRelativeId(clusterName, backupNamespace)));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(
        eq(profileName),
        eq(StackGresUtil.getNamespaceFromRelativeId(clusterName, backupNamespace)));
  }

  @DisplayName("Given a SGCluster with a valid SGObjectStorage should not fail")
  void testValidObjectStorageConfiguration() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = backup.getSpec().getSgShardedCluster();
    final StackGresShardedClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfigurations clusterConfiguration =
        clusterSpec.getCoordinator().getConfigurations();
    var backupConfiguration = new StackGresClusterBackupConfiguration();
    clusterConfiguration.setBackups(
        List.of(backupConfiguration));
    backupConfiguration.setSgObjectStorage(objectStorage.getMetadata().getName());

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
  }

  @Test
  @DisplayName("Given a backup with invalid SGCluster should fail")
  void testBackupInvalidSgCluster() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String clusterName = backup.getSpec().getSgShardedCluster();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());

    generator.getRequiredResources(backup);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(profileFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  @DisplayName("Given a SgCluster with an invalid SgGObjectStorage should fail")
  void testSgCLusterInvalidSgObjectStorage() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgShardedCluster();
    final String profileName = cluster.getSpec().getCoordinator().getSgInstanceProfile();
    final StackGresShardedClusterSpec clusterSpec = cluster.getSpec();
    final StackGresClusterConfigurations clusterConfiguration =
        clusterSpec.getCoordinator().getConfigurations();
    var backupConfiguration = new StackGresClusterBackupConfiguration();
    clusterConfiguration.setBackups(
        List.of(backupConfiguration));
    backupConfiguration.setSgObjectStorage(objectStorage.getMetadata().getName());

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(objectStorageFinder.findByNameAndNamespace(
        objectStorage.getMetadata().getName(), backupNamespace)).thenReturn(Optional.empty());

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    assertException("SGShardedBackup " + backupNamespace + "." + backupName
        + " target SGShardedCluster " + clusterName
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
    final String clusterName = backup.getSpec().getSgShardedCluster();
    final String profileName = cluster.getSpec().getCoordinator().getSgInstanceProfile();

    cluster.getSpec().setConfigurations(null);

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    assertException("SGShardedBackup " + backupNamespace + "." + backupName
        + " target SGShardedCluster " + clusterName
        + " without a SGObjectStorage");

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(backupNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(backupNamespace));
    verify(objectStorageFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenABackupWithClusterWithoutProfile_getRequiredResourcesShouldFail() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final String backupName = backup.getMetadata().getName();
    final String clusterName = backup.getSpec().getSgShardedCluster();
    final String profileName = cluster.getSpec().getCoordinator().getSgInstanceProfile();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(objectStorageFinder.findByNameAndNamespace(
        objectStorage.getMetadata().getName(), backupNamespace))
            .thenReturn(Optional.of(objectStorage));

    assertException("SGShardedBackup " + backupNamespace + "." + backupName
        + " target SGShardedCluster " + clusterName
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
