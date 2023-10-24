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
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterStatsDto;
import io.stackgres.apiweb.transformer.ShardedClusterStatsTransformer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.PersistentVolumeClaimFinder;
import io.stackgres.common.resource.PodExecutor;
import io.stackgres.common.resource.PodFinder;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ShardedClusterStatsDtoFinder
    extends AbstractClusterStatsDtoFinder<ShardedClusterStatsDto, StackGresShardedCluster> {

  private final CustomResourceScanner<StackGresCluster> clusterScanner;
  private final ManagedExecutor managedExecutor;
  private final PodFinder podFinder;
  private final ShardedClusterLabelFactory shardedClusterLabelFactory;
  private final ClusterLabelFactory clusterLabelFactory;
  private final ShardedClusterStatsTransformer shardedClusterStatsTransformer;

  @Inject
  public ShardedClusterStatsDtoFinder(
      CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      ManagedExecutor managedExecutor,
      PodFinder podFinder,
      PodExecutor podExecutor,
      PersistentVolumeClaimFinder persistentVolumeClaimFinder,
      ShardedClusterLabelFactory shardedClusterLabelFactory,
      ClusterLabelFactory clusterLabelFactory,
      ShardedClusterStatsTransformer shardedClusterStatsTransformer) {
    super(shardedClusterFinder, podExecutor, persistentVolumeClaimFinder);
    this.clusterScanner = clusterScanner;
    this.managedExecutor = managedExecutor;
    this.podFinder = podFinder;
    this.shardedClusterLabelFactory = shardedClusterLabelFactory;
    this.clusterLabelFactory = clusterLabelFactory;
    this.shardedClusterStatsTransformer = shardedClusterStatsTransformer;
  }

  @Override
  protected ShardedClusterStatsDto getClusterStats(StackGresShardedCluster shardedCluster) {
    List<Tuple2<StackGresCluster, Pod>> coordinatorPods = clusterScanner.getResourcesWithLabels(
        shardedCluster.getMetadata().getNamespace(),
        shardedClusterLabelFactory.coordinatorLabels(shardedCluster))
        .stream()
        .flatMap(cluster -> podFinder.findByLabelsAndNamespace(
            cluster.getMetadata().getNamespace(),
            clusterLabelFactory.clusterLabels(cluster))
            .stream()
            .map(pod -> Tuple.tuple(cluster, pod)))
        .toList();

    List<PodStats> allCoordinatorPodStats = coordinatorPods
        .stream()
        .map(t -> CompletableFuture.supplyAsync(() -> t.skip1().concat(getPodStats(t.v2))
            .concat(getPodPersitentVolumeClaim(t.v1, t.v2)), managedExecutor))
        .map(CompletableFuture::join)
        .map(PodStats::fromTuple)
        .toList();

    List<Tuple2<StackGresCluster, Pod>> shardsPods = clusterScanner.getResourcesWithLabels(
        shardedCluster.getMetadata().getNamespace(),
        shardedClusterLabelFactory.shardsLabels(shardedCluster))
        .stream()
        .flatMap(cluster -> podFinder.findByLabelsAndNamespace(
            cluster.getMetadata().getNamespace(),
            clusterLabelFactory.clusterLabels(cluster))
            .stream()
            .map(pod -> Tuple.tuple(cluster, pod)))
        .toList();

    List<PodStats> allShardsPodStats = shardsPods
        .stream()
        .map(t -> CompletableFuture.supplyAsync(() -> t.skip1().concat(getPodStats(t.v2))
            .concat(getPodPersitentVolumeClaim(t.v1, t.v2)), managedExecutor))
        .map(CompletableFuture::join)
        .map(PodStats::fromTuple)
        .toList();

    return shardedClusterStatsTransformer.toDtoWithAllPodStats(shardedCluster,
        allCoordinatorPodStats, allShardsPodStats);
  }

}
