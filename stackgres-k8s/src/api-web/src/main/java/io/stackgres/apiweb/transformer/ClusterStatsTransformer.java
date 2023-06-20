/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.apiweb.dto.cluster.ClusterStatsDto;
import io.stackgres.apiweb.resource.PodStats;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

@ApplicationScoped
public class ClusterStatsTransformer
    extends AbstractClusterStatsTransformer<ClusterStatsDto, StackGresCluster> {

  @Inject
  public ClusterStatsTransformer(ClusterPodTransformer clusterPodTransformer) {
    super(clusterPodTransformer);
  }

  public ClusterStatsDto toDtoWithAllPodStats(
      StackGresCluster source, List<PodStats> allPodStats) {
    ClusterStatsDto clusterStatsDto = toDto(source);

    setAllStats(clusterStatsDto, allPodStats);

    return clusterStatsDto;
  }

  @Override
  public ClusterStatsDto toDto(StackGresCluster source) {
    ClusterStatsDto transformation = new ClusterStatsDto();
    transformation.setMetadata(getDtoMetadata(source));
    return transformation;
  }

}
