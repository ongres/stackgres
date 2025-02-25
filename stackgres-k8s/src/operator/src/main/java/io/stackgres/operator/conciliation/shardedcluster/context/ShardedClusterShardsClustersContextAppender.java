/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.List;
import java.util.stream.IntStream;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForDdpUtil;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForShardingSphereUtil;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterShardsClustersContextAppender
    extends ContextAppender<StackGresShardedCluster, Builder> {

  private final ShardedClusterShardsPrimaryEndpointsContextAppender
      shardedClusterShardsPrimaryEndpointsContextAppender;

  public ShardedClusterShardsClustersContextAppender(
      ShardedClusterShardsPrimaryEndpointsContextAppender
          shardedClusterShardsPrimaryEndpointsContextAppender) {
    this.shardedClusterShardsPrimaryEndpointsContextAppender =
        shardedClusterShardsPrimaryEndpointsContextAppender;
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    List<StackGresCluster> shards = getShardsClusters(cluster);
    contextBuilder.shards(shards);
    shardedClusterShardsPrimaryEndpointsContextAppender.appendContext(shards, contextBuilder);
  }

  private List<StackGresCluster> getShardsClusters(StackGresShardedCluster cluster) {
    return IntStream.range(0, cluster.getSpec().getShards().getClusters())
        .mapToObj(index -> getShardsCluster(cluster, index))
        .toList();
  }

  private StackGresCluster getShardsCluster(StackGresShardedCluster cluster, int index) {
    switch (StackGresShardingType.fromString(cluster.getSpec().getType())) {
      case CITUS:
        return StackGresShardedClusterForCitusUtil.getShardsCluster(cluster, index);
      case DDP:
        return StackGresShardedClusterForDdpUtil.getShardsCluster(cluster, index);
      case SHARDING_SPHERE:
        return StackGresShardedClusterForShardingSphereUtil.getShardsCluster(cluster, index);
      default:
        throw new UnsupportedOperationException(
            "Sharding technology " + cluster.getSpec().getType() + " not implemented");
    }
  }

}
