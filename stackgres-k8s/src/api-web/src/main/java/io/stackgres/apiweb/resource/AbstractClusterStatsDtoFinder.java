/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;
import java.util.Optional;

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
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.PersistentVolumeClaimFinder;
import io.stackgres.common.resource.PodExecutor;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractClusterStatsDtoFinder<R, T extends CustomResource<?, ?>>
    implements CustomResourceFinder<R> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClusterStatsDtoFinder.class);

  private final CustomResourceFinder<T> clusterFinder;
  private final PodExecutor podExecutor;
  private final PersistentVolumeClaimFinder persistentVolumeClaimFinder;

  @Inject
  protected AbstractClusterStatsDtoFinder(
      CustomResourceFinder<T> clusterFinder,
      PodExecutor podExecutor,
      PersistentVolumeClaimFinder persistentVolumeClaimFinder) {
    this.clusterFinder = clusterFinder;
    this.podExecutor = podExecutor;
    this.persistentVolumeClaimFinder = persistentVolumeClaimFinder;
  }

  protected AbstractClusterStatsDtoFinder() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.clusterFinder = null;
    this.podExecutor = null;
    this.persistentVolumeClaimFinder = null;
  }

  @Override
  public Optional<R> findByNameAndNamespace(
      String name, String namespace) {
    return clusterFinder.findByNameAndNamespace(name, namespace)
        .map(this::getClusterStats);
  }

  protected abstract R getClusterStats(T cluster);

  protected ImmutableMap<PatroniStatsScripts, String> getPodStats(Pod pod) {
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

  protected Optional<PersistentVolumeClaim> getPodPersitentVolumeClaim(
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
