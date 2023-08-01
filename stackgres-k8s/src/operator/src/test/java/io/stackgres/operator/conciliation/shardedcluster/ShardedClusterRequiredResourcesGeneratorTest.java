/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShardedClusterRequiredResourcesGeneratorTest
    extends AbstractShardedClusterRequiredResourcesGeneratorTest {

  @Test
  void givenValidCluster_getRequiredResourcesShouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockProfile();
    mockPgConfig();
    mockPoolingConfig();

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getPostgresConfig(),
        clusterNamespace);
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getConnectionPoolingConfig(),
        clusterNamespace);
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getResourceProfile(), clusterNamespace);
  }

  @Test
  void givenAClusterWithInvalidCoordinatorPgConfig_getRequiredResourcesShouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    cluster.getSpec().getCoordinator().getConfiguration().setPostgresConfig("test");
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getPostgresConfig(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("Coordinator of SGShardedCluster " + clusterNamespace + "." + clusterName
        + " have a non existent SGPostgresConfig "
        + cluster.getSpec().getCoordinator().getConfiguration().getPostgresConfig());

    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getPostgresConfig(),
        clusterNamespace);
    verify(profileConfigFinder, never()).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenAClusterWithInvalidShardsPgConfig_getRequiredResourcesShouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    mockProfile();
    mockPgConfig();
    mockPoolingConfig();
    cluster.getSpec().getShards().getConfiguration().setPostgresConfig("test");
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getShards().getConfiguration().getPostgresConfig(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("Shards of SGShardedCluster " + clusterNamespace + "." + clusterName
        + " have a non existent SGPostgresConfig "
        + cluster.getSpec().getShards().getConfiguration().getPostgresConfig());

    verify(postgresConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getPostgresConfig(),
        clusterNamespace);
    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getShards().getConfiguration().getPostgresConfig(),
        clusterNamespace);
    verify(profileConfigFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(poolingConfigFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenAClusterWithoutShardsInstanceProfile_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    mockProfile();
    mockPgConfig();
    mockPoolingConfig();
    cluster.getSpec().getShards().setResourceProfile("test");
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getShards().getResourceProfile(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("Shards of SGShardedCluster " + clusterNamespace + "."
        + clusterName + " have a non existent SGInstanceProfile "
        + cluster.getSpec().getShards().getResourceProfile());

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getPostgresConfig(),
        clusterNamespace);
    verify(profileConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getResourceProfile(), clusterNamespace);
    verify(profileConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getShards().getResourceProfile(), clusterNamespace);
    verify(poolingConfigFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenAClusterWithoutCoordinatorInstanceProfile_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    mockPgConfig();
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getResourceProfile(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("Coordinator of SGShardedCluster " + clusterNamespace + "."
        + clusterName + " have a non existent SGInstanceProfile "
        + cluster.getSpec().getCoordinator().getResourceProfile());

    verify(postgresConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getPostgresConfig(),
        clusterNamespace);
    verify(profileConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getResourceProfile(), clusterNamespace);
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithNoPoolingConfig_shouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockProfile();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getConnectionPoolingConfig(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getPostgresConfig(),
        clusterNamespace);
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfiguration().getConnectionPoolingConfig(),
        clusterNamespace);
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getResourceProfile(),
        clusterNamespace);
  }

}
