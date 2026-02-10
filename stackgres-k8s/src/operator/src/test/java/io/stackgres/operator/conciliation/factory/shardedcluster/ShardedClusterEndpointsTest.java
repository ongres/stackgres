/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterEndpointsTest {

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private StackGresShardedClusterContext context;

  private ShardedClusterEndpoints shardedClusterEndpoints;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    shardedClusterEndpoints = new ShardedClusterEndpoints(labelFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);
    when(context.getCoordinatorPrimaryEndpoints()).thenReturn(Optional.empty());
    when(context.getShardsPrimaryEndpoints()).thenReturn(List.of());
  }

  @Test
  void generateResource_shouldGenerateEndpoints() {
    List<HasMetadata> resources =
        shardedClusterEndpoints.generateResource(context).toList();

    assertFalse(resources.isEmpty());
    assertEquals(2, resources.size());
  }

  @Test
  void generateResource_shouldHaveCorrectCoordinatorEndpointsName() {
    List<HasMetadata> resources =
        shardedClusterEndpoints.generateResource(context).toList();

    String expectedName =
        StackGresShardedClusterUtil.primaryCoordinatorServiceName(cluster);
    HasMetadata coordinatorEndpoints = resources.stream()
        .filter(r -> r.getMetadata().getName().equals(expectedName))
        .findFirst()
        .orElseThrow();
    assertEquals(expectedName, coordinatorEndpoints.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(),
        coordinatorEndpoints.getMetadata().getNamespace());
  }

  @Test
  void generateResource_shouldHaveCorrectShardsEndpointsName() {
    List<HasMetadata> resources =
        shardedClusterEndpoints.generateResource(context).toList();

    String expectedName =
        StackGresShardedClusterUtil.primariesShardsServiceName(cluster);
    HasMetadata shardsEndpoints = resources.stream()
        .filter(r -> r.getMetadata().getName().equals(expectedName))
        .findFirst()
        .orElseThrow();
    assertEquals(expectedName, shardsEndpoints.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(),
        shardsEndpoints.getMetadata().getNamespace());
  }

}
