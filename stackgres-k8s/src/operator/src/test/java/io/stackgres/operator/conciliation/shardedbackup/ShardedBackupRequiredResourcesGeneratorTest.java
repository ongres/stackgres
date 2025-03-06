/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurationsBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfigurationBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ConfigScanner;
import io.stackgres.common.resource.ObjectStorageFinder;
import io.stackgres.common.resource.ProfileFinder;
import io.stackgres.common.resource.ShardedClusterFinder;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShardedBackupRequiredResourcesGeneratorTest {

  @InjectMock
  ConfigScanner configScanner;

  @InjectMock
  ShardedClusterFinder shardedClusterFinder;

  @InjectMock
  ClusterFinder clusterFinder;

  @InjectMock
  ProfileFinder profileFinder;

  @InjectMock
  ObjectStorageFinder objectStorageFinder;

  @Inject
  ShardedBackupRequiredResourcesGenerator generator;

  private StackGresConfig config;
  private StackGresShardedBackup backup;
  private StackGresObjectStorage objectStorage;
  private StackGresCluster coordinator;
  private StackGresProfile profile;
  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    backup = Fixtures.shardedBackup().loadDefault().get();
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
    coordinator = Fixtures.cluster().loadDefault().get();
    profile = Fixtures.instanceProfile().loadSizeS().get();
  }

  @Test
  void givenValidBackup_shouldPass() {
    final String backupNamespace = backup.getMetadata().getNamespace();
    final StackGresShardedClusterSpec clusterSpec = cluster.getSpec();
    clusterSpec.getCoordinator().setConfigurations(
        new StackGresClusterConfigurationsBuilder()
        .addNewBackup()
        .withSgObjectStorage(objectStorage.getMetadata().getName())
        .endBackup()
        .build());

    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));

    when(shardedClusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(coordinator));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    when(objectStorageFinder.findByNameAndNamespace(
        objectStorage.getMetadata().getName(), backupNamespace))
            .thenReturn(Optional.of(objectStorage));

    generator.getRequiredResources(backup);
  }

}
