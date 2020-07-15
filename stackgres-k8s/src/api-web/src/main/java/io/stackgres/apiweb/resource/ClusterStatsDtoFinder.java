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
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.apiweb.dto.cluster.ClusterStatsDto;
import io.stackgres.apiweb.transformer.ClusterStatsTransformer;
import io.stackgres.common.ClusterLabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.PersistentVolumeClaimFinder;
import io.stackgres.common.resource.PodExecutor;
import io.stackgres.common.resource.PodFinder;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterStatsDtoFinder
    implements CustomResourceFinder<ClusterStatsDto> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatsDtoFinder.class);

  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final PodFinder podFinder;
  private final PodExecutor podExecutor;
  private final PersistentVolumeClaimFinder persistentVolumeClaimFinder;
  private final ClusterLabelFactory clusterLabelFactory;
  private final ClusterStatsTransformer clusterStatsTransformer;

  @Inject
  public ClusterStatsDtoFinder(CustomResourceFinder<StackGresCluster> clusterFinder,
      PodFinder podFinder, PodExecutor podExecutor,
      PersistentVolumeClaimFinder persistentVolumeClaimFinder,
      ClusterLabelFactory clusterLabelFactory, ClusterStatsTransformer clusterStatsTransformer) {
    super();
    this.clusterFinder = clusterFinder;
    this.podFinder = podFinder;
    this.podExecutor = podExecutor;
    this.persistentVolumeClaimFinder = persistentVolumeClaimFinder;
    this.clusterLabelFactory = clusterLabelFactory;
    this.clusterStatsTransformer = clusterStatsTransformer;
  }

  @Override
  public Optional<ClusterStatsDto> findByNameAndNamespace(
      String name, String namespace) {
    return clusterFinder.findByNameAndNamespace(name, namespace)
        .map(this::getClusterStats);
  }

  private ClusterStatsDto getClusterStats(StackGresCluster cluster) {
    List<Pod> pods = podFinder.findResourcesInNamespaceWithLabels(
        cluster.getMetadata().getNamespace(),
        clusterLabelFactory.statefulSetPodLabels(cluster));

    ImmutableList<PodStats> allPodStats = pods
        .stream()
        .parallel()
        .map(pod -> Tuple.tuple(pod))
        .map(t -> t.concat(getPodStats(t.v1)))
        .map(t -> t.concat(getPodPersitentVolumeClaim(cluster, t.v1)))
        .map(PodStats::fromTuple)
        .collect(ImmutableList.toImmutableList());

    return clusterStatsTransformer.toDtoWithAllPodStats(cluster, allPodStats);
  }

  private ImmutableMap<PatroniStatsScripts, String> getPodStats(Pod pod) {
    try {
      return Seq.seq(podExecutor.exec(pod, StackgresClusterContainers.PATRONI, "sh", "-c",
          Seq.seq(PatroniStatsScripts.getScripts())
          .map(tt -> "echo \"" + tt.v1.getName() + ":$( (" + tt.v2 + ") 2>&1 | tr -d '\\n')\"\n")
          .toString()))
          .peek(line -> {
            if (LOGGER.isTraceEnabled() && line.endsWith("#failed")) {
              LOGGER.trace("An error accurred while retrieving stats for pod {}.{}: {}",
                  pod.getMetadata().getNamespace(),
                  pod.getMetadata().getName(),
                  line.substring(0, line.indexOf("#failed")));
            }
          })
          .filter(line -> !line.endsWith("#failed"))
          .map(line -> Tuple.tuple(line, line.indexOf(":")))
          .map(tt -> Tuple.tuple(
              PatroniStatsScripts.fromName(tt.v1.substring(0, tt.v2)),
              tt.v1.substring(tt.v2 + 1)))
          .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));
    } catch (Exception ex) {
      LOGGER.debug("An error accurred while retrieving stats for pod {}.{}: {}",
          pod.getMetadata().getNamespace(),
          pod.getMetadata().getName(),
          ex.getMessage());
      return ImmutableMap.<PatroniStatsScripts, String>of();
    }
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
