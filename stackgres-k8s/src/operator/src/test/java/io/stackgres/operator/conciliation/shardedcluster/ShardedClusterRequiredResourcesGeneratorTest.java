/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

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
    mockProfile();
    mockPgConfig();
    mockPoolConfig();

    generator.getRequiredResources(cluster);
  }

  @Test
  void givenAClusterWithInvalidCoordinatorProfile_getRequiredResourcesShouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockPgConfig();
    mockPoolConfig();

    cluster.getSpec().getCoordinator().setSgInstanceProfile("test");
    when(profileFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getSgInstanceProfile(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("SGInstanceProfile test was not found");
  }

  @Test
  void givenAClusterWithInvalidCoordinatorPostgresConfig_getRequiredResourcesShouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockProfile();
    mockPoolConfig();

    cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .setSgPostgresConfig("test");
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getSgPostgresConfig(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("SGPostgresConfig test was not found");
  }

  @Test
  void givenAClusterWithInvalidCoordinatorPoolingConfig_getRequiredResourcesShouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterNamespace = metadata.getNamespace();

    mockProfile();
    mockPgConfig();

    cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .setSgPoolingConfig("test");
    when(poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getSgPoolingConfig(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("SGPoolingConfig test was not found");
  }

}
