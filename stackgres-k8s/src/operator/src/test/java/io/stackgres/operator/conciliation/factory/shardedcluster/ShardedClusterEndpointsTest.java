/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.EndpointAddressBuilder;
import io.fabric8.kubernetes.api.model.EndpointSubsetBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
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

  @Test
  void generateResource_whenCoordinatorEndpointsPresent_shouldIncludeSubsets() {
    Endpoints coordinatorEndpoints = new EndpointsBuilder()
        .withSubsets(new EndpointSubsetBuilder()
            .withAddresses(new EndpointAddressBuilder()
                .withIp("10.0.0.1")
                .build())
            .build())
        .build();
    lenient().when(context.getCoordinatorPrimaryEndpoints())
        .thenReturn(Optional.of(coordinatorEndpoints));

    List<HasMetadata> resources =
        shardedClusterEndpoints.generateResource(context).toList();

    String expectedName =
        StackGresShardedClusterUtil.primaryCoordinatorServiceName(cluster);
    Endpoints result = resources.stream()
        .filter(r -> r.getMetadata().getName().equals(expectedName))
        .map(Endpoints.class::cast)
        .findFirst()
        .orElseThrow();
    assertNotNull(result.getSubsets());
    assertFalse(result.getSubsets().isEmpty(),
        "Expected coordinator endpoints to have subsets");
    assertEquals("10.0.0.1",
        result.getSubsets().get(0).getAddresses().get(0).getIp());
  }

  @Test
  void generateResource_whenShardEndpointsPresent_shouldIncludeShardSubsets() {
    Endpoints shard0Endpoints = new EndpointsBuilder()
        .withSubsets(new EndpointSubsetBuilder()
            .withAddresses(new EndpointAddressBuilder()
                .withIp("10.0.1.1")
                .build())
            .build())
        .build();
    Endpoints shard1Endpoints = new EndpointsBuilder()
        .withSubsets(new EndpointSubsetBuilder()
            .withAddresses(new EndpointAddressBuilder()
                .withIp("10.0.2.1")
                .build())
            .build())
        .build();
    lenient().when(context.getShardsPrimaryEndpoints())
        .thenReturn(List.of(shard0Endpoints, shard1Endpoints));

    List<HasMetadata> resources =
        shardedClusterEndpoints.generateResource(context).toList();

    String expectedName =
        StackGresShardedClusterUtil.primariesShardsServiceName(cluster);
    Endpoints result = resources.stream()
        .filter(r -> r.getMetadata().getName().equals(expectedName))
        .map(Endpoints.class::cast)
        .findFirst()
        .orElseThrow();
    assertNotNull(result.getSubsets());
    assertEquals(2, result.getSubsets().size(),
        "Expected shard endpoints to have 2 subsets");
    assertTrue(result.getSubsets().stream()
            .flatMap(s -> s.getAddresses().stream())
            .anyMatch(a -> "10.0.1.1".equals(a.getIp())),
        "Expected shard 0 IP in subsets");
    assertTrue(result.getSubsets().stream()
            .flatMap(s -> s.getAddresses().stream())
            .anyMatch(a -> "10.0.2.1".equals(a.getIp())),
        "Expected shard 1 IP in subsets");
  }

}
