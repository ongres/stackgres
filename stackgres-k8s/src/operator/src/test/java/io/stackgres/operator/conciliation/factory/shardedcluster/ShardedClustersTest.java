/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class ShardedClustersTest {

  @Mock
  private StackGresShardedClusterContext context;
  @Mock
  private StackGresClusterContext coordinatorContext;
  @Mock
  private StackGresClusterContext shard0Context;
  @Mock
  private StackGresClusterContext shard1Context;

  private LabelFactoryForShardedCluster labelFactory;
  private StackGresShardedCluster shardedCluster;
  private StackGresCluster coordinator;
  private StackGresCluster shard0;
  private StackGresCluster shard1;
  private ShardedClusters shardedClusters;

  @BeforeEach
  public void setup() {
    openMocks(this);
    shardedCluster = Fixtures.shardedCluster().loadDefault().get();

    labelFactory = new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());
    coordinator = Fixtures.cluster().loadDefault().get();
    when(coordinatorContext.getCluster()).thenReturn(coordinator);
    when(coordinatorContext.getSource()).thenReturn(coordinator);
    shard0 = Fixtures.cluster().loadDefault().get();
    when(shard0Context.getCluster()).thenReturn(shard0);
    when(shard0Context.getSource()).thenReturn(shard0);
    shard1 = Fixtures.cluster().loadDefault().get();
    when(shard1Context.getCluster()).thenReturn(shard1);
    when(shard1Context.getSource()).thenReturn(shard1);
    shardedClusters = new ShardedClusters(labelFactory);
    when(context.getSource()).thenReturn(shardedCluster);
    when(context.getCoordinator()).thenReturn(coordinatorContext);
    when(context.getShards()).thenReturn(List.of(shard0Context, shard1Context));
  }

  @Test
  public void generateShardedClusters_shouldSetLabels() {
    var clusters = shardedClusters.generateResource(context).toList();
    assertEquals(3, clusters.size());
    assertEquals(labelFactory.coordinatorLabels(shardedCluster),
        clusters.getFirst().getMetadata().getLabels());
    assertEquals(labelFactory.shardsLabels(shardedCluster),
        clusters.get(1).getMetadata().getLabels());
    assertEquals(labelFactory.shardsLabels(shardedCluster),
        clusters.get(2).getMetadata().getLabels());
  }

}
