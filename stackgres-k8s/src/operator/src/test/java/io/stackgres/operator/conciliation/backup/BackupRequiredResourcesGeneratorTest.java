/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ObjectStorageFinder;
import io.stackgres.common.resource.ProfileFinder;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
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
  void givenValidBackup_shouldPass() {
    final String backupNamespace = backup.getMetadata().getNamespace();
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
  }

}
