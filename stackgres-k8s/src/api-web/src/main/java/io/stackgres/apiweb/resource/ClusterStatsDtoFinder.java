/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateRunning;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.apiweb.dto.cluster.ClusterStatsDto;
import io.stackgres.apiweb.transformer.ClusterStatsTransformer;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.PersistentVolumeClaimFinder;
import io.stackgres.common.resource.PodExecutor;
import io.stackgres.common.resource.PodFinder;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterStatsDtoFinder
    implements CustomResourceFinder<ClusterStatsDto> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatsDtoFinder.class);

  private ManagedExecutor managedExecutor;
  private CustomResourceFinder<StackGresCluster> clusterFinder;
  private PodFinder podFinder;
  private PodExecutor podExecutor;
  private PersistentVolumeClaimFinder persistentVolumeClaimFinder;
  private ClusterLabelFactory clusterLabelFactory;
  private ClusterStatsTransformer clusterStatsTransformer;

  @Override
  public Optional<ClusterStatsDto> findByNameAndNamespace(
      String name, String namespace) {
    return clusterFinder.findByNameAndNamespace(name, namespace)
        .map(this::getClusterStats);
  }

  private ClusterStatsDto getClusterStats(StackGresCluster cluster) {
    List<Pod> pods = podFinder.findByLabelsAndNamespace(
        cluster.getMetadata().getNamespace(),
        clusterLabelFactory.patroniClusterLabels(cluster));

    ImmutableList<PodStats> allPodStats = pods
        .stream()
        .map(Tuple::tuple)
        .map(t -> CompletableFuture.supplyAsync(() -> t.concat(getPodStats(t.v1))
            .concat(getPodPersitentVolumeClaim(cluster, t.v1)), managedExecutor))
        .map(CompletableFuture::join)
        .map(PodStats::fromTuple)
        .collect(ImmutableList.toImmutableList());

    return clusterStatsTransformer.toDtoWithAllPodStats(cluster, allPodStats);
  }

  private ImmutableMap<PatroniStatsScripts, String> getPodStats(Pod pod) {
    try {
      if (Optional.ofNullable(pod.getStatus())
          .map(PodStatus::getContainerStatuses).stream()
          .flatMap(List::stream)
          .noneMatch(container -> container.getName().equals(StackGresContainer.PATRONI.getName())
              && Optional.of(container).map(ContainerStatus::getState)
                  .map(ContainerState::getRunning)
                  .map(ContainerStateRunning::getStartedAt).isPresent()
              && Optional.of(container).map(ContainerStatus::getState)
                  .map(ContainerState::getTerminated)
                  .map(ContainerStateTerminated::getFinishedAt).isEmpty())) {
        LOGGER.debug("Patroni container is not running, could not retrieve stats for pod {}.{}",
            pod.getMetadata().getNamespace(),
            pod.getMetadata().getName());
        return ImmutableMap.<PatroniStatsScripts, String>of();
      }

      return Seq.seq(podExecutor.exec(pod, StackGresContainer.PATRONI.getName(), "sh", "-c",
          Seq.seq(PatroniStatsScripts.getScripts())
              .map(tt -> "echo \"" + tt.v1.getName() + ":$( (" + tt.v2
                  + ") 2>&1 | tr -d '\\n')\"\n")
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
    } catch (RuntimeException ex) {
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

  @Inject
  public void setManagedExecutor(ManagedExecutor managedExecutor) {
    this.managedExecutor = managedExecutor;
  }

  @Inject
  public void setClusterFinder(CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Inject
  public void setPodFinder(PodFinder podFinder) {
    this.podFinder = podFinder;
  }

  @Inject
  public void setPodExecutor(PodExecutor podExecutor) {
    this.podExecutor = podExecutor;
  }

  @Inject
  public void setPersistentVolumeClaimFinder(
      PersistentVolumeClaimFinder persistentVolumeClaimFinder) {
    this.persistentVolumeClaimFinder = persistentVolumeClaimFinder;
  }

  @Inject
  public void setClusterLabelFactory(ClusterLabelFactory clusterLabelFactory) {
    this.clusterLabelFactory = clusterLabelFactory;
  }

  @Inject
  public void setClusterStatsTransformer(ClusterStatsTransformer clusterStatsTransformer) {
    this.clusterStatsTransformer = clusterStatsTransformer;
  }
}
