/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.apiweb.dto.cluster.ClusterStatsDto;
import io.stackgres.apiweb.transformer.ClusterStatsTransformer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.PersistentVolumeClaimFinder;
import io.stackgres.common.resource.PodExecutor;
import io.stackgres.common.resource.PodFinder;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jooq.lambda.tuple.Tuple;

@ApplicationScoped
public class ClusterStatsDtoFinder
    extends AbstractClusterStatsDtoFinder<ClusterStatsDto, StackGresCluster> {

  private final ManagedExecutor managedExecutor;
  private final PodFinder podFinder;
  private final ClusterLabelFactory clusterLabelFactory;
  private final ClusterStatsTransformer clusterStatsTransformer;

  @Inject
  public ClusterStatsDtoFinder(
      ManagedExecutor managedExecutor,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      PodFinder podFinder,
      PodExecutor podExecutor,
      PersistentVolumeClaimFinder persistentVolumeClaimFinder,
      ClusterLabelFactory clusterLabelFactory,
      ClusterStatsTransformer clusterStatsTransformer) {
    super(clusterFinder, podExecutor, persistentVolumeClaimFinder);
    this.managedExecutor = managedExecutor;
    this.podFinder = podFinder;
    this.clusterLabelFactory = clusterLabelFactory;
    this.clusterStatsTransformer = clusterStatsTransformer;
  }

  @Override
  protected ClusterStatsDto getClusterStats(StackGresCluster cluster) {
    List<Pod> pods = podFinder.findByLabelsAndNamespace(
        cluster.getMetadata().getNamespace(),
        clusterLabelFactory.clusterLabels(cluster));

    List<PodStats> allPodStats = pods
        .stream()
        .map(Tuple::tuple)
        .map(t -> CompletableFuture.supplyAsync(() -> t.concat(getPodStats(t.v1))
            .concat(getPodPersitentVolumeClaim(cluster, t.v1)), managedExecutor))
        .map(CompletableFuture::join)
        .map(PodStats::fromTuple)
        .toList();

    return clusterStatsTransformer.toDtoWithAllPodStats(cluster, allPodStats);
  }

}
