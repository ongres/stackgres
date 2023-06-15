/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterClusterStats;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterStatsDto;
import io.stackgres.apiweb.resource.PodStats;
import io.stackgres.apiweb.transformer.util.PodStatsUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.ResourceUtil;

@ApplicationScoped
public class ShardedClusterStatsTransformer
    extends AbstractDtoTransformer<ShardedClusterStatsDto, StackGresShardedCluster> {

  public ShardedClusterStatsDto toDtoWithAllPodStats(
      StackGresShardedCluster source,
      List<PodStats> allCoordinatorPodStats,
      List<PodStats> allShardsPodStats) {
    ShardedClusterStatsDto shardedClusterStatsDto = toDto(source);

    setAllStats(shardedClusterStatsDto, allCoordinatorPodStats, allShardsPodStats);

    return shardedClusterStatsDto;
  }

  @Override
  public ShardedClusterStatsDto toDto(StackGresShardedCluster source) {
    ShardedClusterStatsDto transformation = new ShardedClusterStatsDto();
    transformation.setMetadata(getDtoMetadata(source));
    return transformation;
  }

  private void setAllStats(
      ShardedClusterStatsDto stats,
      List<PodStats> allCoordinatorPodStats,
      List<PodStats> allShardsPodStats) {
    stats.setCoordinator(new ShardedClusterClusterStats());
    setGlobalRequested(stats, ShardedClusterStatsDto::getCoordinator, allCoordinatorPodStats);
    stats.setShards(new ShardedClusterClusterStats());
    setGlobalRequested(stats, ShardedClusterStatsDto::getShards, allShardsPodStats);
  }

  private void setGlobalRequested(ShardedClusterStatsDto stats,
      Function<ShardedClusterStatsDto, ShardedClusterClusterStats> mapper,
      List<PodStats> allPodStats) {
    mapper.apply(stats).setCpuRequested(allPodStats
        .stream()
        .map(PodStatsUtil::getPodCpuRequested)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .reduce(BigInteger::add)
        .map(ResourceUtil::asMillicpusWithUnit)
        .orElse(null));
    mapper.apply(stats).setMemoryRequested(allPodStats
        .stream()
        .map(PodStatsUtil::getPodMemoryRequested)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .reduce(BigInteger::add)
        .map(ResourceUtil::asBytesWithUnit)
        .orElse(null));
    mapper.apply(stats).setDiskRequested(allPodStats
        .stream()
        .map(PodStatsUtil::getPodDiskRequested)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .reduce(BigInteger::add)
        .map(ResourceUtil::asBytesWithUnit)
        .orElse(null));
  }

}
