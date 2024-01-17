/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

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

    mockPgConfig();

    generator.getRequiredResources(cluster);

    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig(),
        clusterNamespace);
  }

  @Test
  void givenAClusterWithInvalidCoordinatorPgConfig_getRequiredResourcesShouldFail() {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .setSgPostgresConfig("test");
    when(postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator()
        .getSgPostgresConfig(),
        clusterNamespace))
        .thenReturn(Optional.empty());

    assertException("Coordinator of SGShardedCluster " + clusterNamespace + "." + clusterName
        + " have a non existent SGPostgresConfig "
        + cluster.getSpec().getCoordinator()
        .getConfigurationsForCoordinator().getSgPostgresConfig());

    verify(postgresConfigFinder, times(1)).findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig(),
        clusterNamespace);
  }

}
