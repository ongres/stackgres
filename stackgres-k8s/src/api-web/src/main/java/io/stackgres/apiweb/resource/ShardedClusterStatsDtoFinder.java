/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterStatsDto;
import io.stackgres.apiweb.transformer.ShardedClusterStatsTransformer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.PersistentVolumeClaimFinder;
import io.stackgres.common.resource.PodFinder;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ShardedClusterStatsDtoFinder
    implements CustomResourceFinder<ShardedClusterStatsDto> {

  private final CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder;
  private final CustomResourceScanner<StackGresCluster> clusterScanner;
  private final PodFinder podFinder;
  private final PersistentVolumeClaimFinder persistentVolumeClaimFinder;
  private final ShardedClusterLabelFactory shardedClusterLabelFactory;
  private final ClusterLabelFactory clusterLabelFactory;
  private final ShardedClusterStatsTransformer shardedClusterStatsTransformer;

  @Inject
  public ShardedClusterStatsDtoFinder(
      CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      PodFinder podFinder,
      PersistentVolumeClaimFinder persistentVolumeClaimFinder,
      ShardedClusterLabelFactory shardedClusterLabelFactory,
      ClusterLabelFactory clusterLabelFactory,
      ShardedClusterStatsTransformer shardedClusterStatsTransformer) {
    this.shardedClusterFinder = shardedClusterFinder;
    this.clusterScanner = clusterScanner;
    this.podFinder = podFinder;
    this.persistentVolumeClaimFinder = persistentVolumeClaimFinder;
    this.shardedClusterLabelFactory = shardedClusterLabelFactory;
    this.clusterLabelFactory = clusterLabelFactory;
    this.shardedClusterStatsTransformer = shardedClusterStatsTransformer;
  }

  @Override
  public Optional<ShardedClusterStatsDto> findByNameAndNamespace(
      String name, String namespace) {
    return shardedClusterFinder.findByNameAndNamespace(name, namespace)
        .map(this::getShardedClusterStats);
  }

  private ShardedClusterStatsDto getShardedClusterStats(StackGresShardedCluster shardedCluster) {
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

    ImmutableList<PodStats> allCoordinatorPodStats = coordinatorPods
        .stream()
        .map(t -> t.concat(getPodPersitentVolumeClaim(t.v1, t.v2)).skip1())
        .map(PodStats::fromTuple)
        .collect(ImmutableList.toImmutableList());

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

    ImmutableList<PodStats> allShardsPodStats = shardsPods
        .stream()
        .map(t -> t.concat(getPodPersitentVolumeClaim(t.v1, t.v2)).skip1())
        .map(PodStats::fromTuple)
        .collect(ImmutableList.toImmutableList());

    return shardedClusterStatsTransformer.toDtoWithAllPodStats(shardedCluster,
        allCoordinatorPodStats, allShardsPodStats);
  }

  private Optional<PersistentVolumeClaim> getPodPersitentVolumeClaim(
      StackGresCluster cluster, Pod pod) {
    return Optional.of(pod)
        .map(Pod::getSpec)
        .map(PodSpec::getVolumes)
        .flatMap(volumes -> Seq.seq(volumes)
            .filter(volumeName -> volumeName.getName().equals(
                StackGresUtil.statefulSetDataPersistentVolumeName(cluster)))
            .findAny())
        .filter(volume -> volume.getPersistentVolumeClaim() != null)
        .map(Volume::getPersistentVolumeClaim)
        .map(PersistentVolumeClaimVolumeSource::getClaimName)
        .flatMap(podDataPvcName -> persistentVolumeClaimFinder
            .findByNameAndNamespace(podDataPvcName, pod.getMetadata().getNamespace()));
  }

}
