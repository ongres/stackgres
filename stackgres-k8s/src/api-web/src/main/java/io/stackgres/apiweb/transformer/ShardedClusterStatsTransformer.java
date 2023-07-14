/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;

import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterClusterStats;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterStatsDto;
import io.stackgres.apiweb.resource.PodStats;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedClusterStatsTransformer
    extends AbstractClusterStatsTransformer<ShardedClusterStatsDto, StackGresShardedCluster> {

  @Inject
  public ShardedClusterStatsTransformer(ClusterPodTransformer clusterPodTransformer) {
    super(clusterPodTransformer);
  }

  public ShardedClusterStatsDto toDtoWithAllPodStats(
      StackGresShardedCluster source,
      List<PodStats> allCoordinatorPodStats,
      List<PodStats> allShardsPodStats) {
    ShardedClusterStatsDto shardedClusterStatsDto = toDto(source);

    shardedClusterStatsDto.setCoordinator(new ShardedClusterClusterStats());
    setAllStats(shardedClusterStatsDto.getCoordinator(), allCoordinatorPodStats);

    shardedClusterStatsDto.setShards(new ShardedClusterClusterStats());
    setAllStats(shardedClusterStatsDto.getShards(), allShardsPodStats);

    return shardedClusterStatsDto;
  }

  @Override
  public ShardedClusterStatsDto toDto(StackGresShardedCluster source) {
    ShardedClusterStatsDto transformation = new ShardedClusterStatsDto();
    transformation.setMetadata(getDtoMetadata(source));
    return transformation;
  }

}
