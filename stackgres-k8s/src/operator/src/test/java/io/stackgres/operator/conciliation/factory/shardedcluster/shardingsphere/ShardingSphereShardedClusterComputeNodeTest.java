/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.shardingsphere;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.external.shardingsphere.ComputeNode;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphere;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereMode;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereZooKeeper;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardingSphereShardedClusterComputeNodeTest {

  @Mock
  private LabelFactoryForShardedCluster labelFactory;

  @Mock
  private StackGresShardedClusterContext context;

  private ShardingSphereShardedClusterComputeNode computeNode;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    computeNode = new ShardingSphereShardedClusterComputeNode(labelFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getShardedCluster()).thenReturn(cluster);
    lenient().when(labelFactory.coordinatorLabels(any())).thenReturn(Map.of());
    lenient().when(labelFactory.coordinatorLabelsWithoutUid(any())).thenReturn(Map.of());
  }

  @Test
  void generateResource_whenTypeIsShardingSphere_shouldGenerateResources() {
    cluster.getSpec().setType(StackGresShardingType.SHARDING_SPHERE.toString());
    setupShardingSphereConfig();

    List<HasMetadata> resources = computeNode.generateResource(context).toList();

    assertFalse(resources.isEmpty());
    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof ComputeNode);
  }

  @Test
  void generateResource_whenTypeIsNotShardingSphere_shouldGenerateNoResources() {
    cluster.getSpec().setType(StackGresShardingType.CITUS.toString());

    List<HasMetadata> resources = computeNode.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  private void setupShardingSphereConfig() {
    final var coordinator = cluster.getSpec().getCoordinator();
    if (coordinator.getConfigurationsForCoordinator() == null) {
      coordinator.setConfigurationsForCoordinator(
          new io.stackgres.common.crd.sgshardedcluster
              .StackGresShardedClusterCoordinatorConfigurations());
    }
    final var shardingSphere =
        new StackGresShardedClusterShardingSphere();
    final var mode =
        new StackGresShardedClusterShardingSphereMode();
    mode.setType("Cluster");
    final var repository =
        new io.stackgres.common.crd.sgshardedcluster
            .StackGresShardedClusterShardingSphereRepository();
    repository.setType("ZooKeeper");
    final var zooKeeper =
        new StackGresShardedClusterShardingSphereZooKeeper();
    zooKeeper.setServerList(List.of("zk-0:2181"));
    repository.setZooKeeper(zooKeeper);
    mode.setRepository(repository);
    shardingSphere.setMode(mode);
    coordinator.getConfigurationsForCoordinator().setShardingSphere(shardingSphere);

    lenient().when(context.getSuperuserUsername()).thenReturn(java.util.Optional.of("postgres"));
    lenient().when(context.getSuperuserPassword()).thenReturn(java.util.Optional.of("password"));
    lenient().when(context.getDatabaseSecret()).thenReturn(java.util.Optional.empty());
    lenient().when(context.getGeneratedSuperuserPassword()).thenReturn("generated-password");
    lenient().when(context.getShardingSphereAuthorityUsers()).thenReturn(List.of());
  }

}
