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
        cluster.getSpec().getCoordinator().getConfigurations().getSgPostgresConfig(),
        clusterNamespace);
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurations().getSgPoolingConfig(),
        clusterNamespace);
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getSgInstanceProfile(), clusterNamespace);
  }

  @Test
  void givenAClusterWithInvalidCoordinatorPgConfig_getRequiredResourcesShouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    cluster.getSpec().getCoordinator().getConfigurations().setSgPostgresConfig("test");
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurations().getSgPostgresConfig(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("Coordinator of SGShardedCluster " + clusterNamespace + "." + clusterName
        + " have a non existent SGPostgresConfig "
        + cluster.getSpec().getCoordinator().getConfigurations().getSgPostgresConfig());

    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurations().getSgPostgresConfig(),
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
    cluster.getSpec().getShards().getConfigurations().setSgPostgresConfig("test");
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("Shards of SGShardedCluster " + clusterNamespace + "." + clusterName
        + " have a non existent SGPostgresConfig "
        + cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig());

    verify(postgresConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurations().getSgPostgresConfig(),
        clusterNamespace);
    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig(),
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
    cluster.getSpec().getShards().setSgInstanceProfile("test");
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getShards().getSgInstanceProfile(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("Shards of SGShardedCluster " + clusterNamespace + "."
        + clusterName + " have a non existent SGInstanceProfile "
        + cluster.getSpec().getShards().getSgInstanceProfile());

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurations().getSgPostgresConfig(),
        clusterNamespace);
    verify(profileConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getSgInstanceProfile(), clusterNamespace);
    verify(profileConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getShards().getSgInstanceProfile(), clusterNamespace);
    verify(poolingConfigFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenAClusterWithoutCoordinatorInstanceProfile_shouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    mockPgConfig();
    when(profileConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getSgInstanceProfile(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("Coordinator of SGShardedCluster " + clusterNamespace + "."
        + clusterName + " have a non existent SGInstanceProfile "
        + cluster.getSpec().getCoordinator().getSgInstanceProfile());

    verify(postgresConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurations().getSgPostgresConfig(),
        clusterNamespace);
    verify(profileConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getSgInstanceProfile(), clusterNamespace);
    verify(poolingConfigFinder, never()).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithNoPoolingConfig_shouldNotFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockProfile();
    mockPgConfig();
    when(poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurations().getSgPoolingConfig(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder, times(3)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurations().getSgPostgresConfig(),
        clusterNamespace);
    verify(poolingConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurations().getSgPoolingConfig(),
        clusterNamespace);
    verify(profileConfigFinder, times(2)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getSgInstanceProfile(),
        clusterNamespace);
  }

}
