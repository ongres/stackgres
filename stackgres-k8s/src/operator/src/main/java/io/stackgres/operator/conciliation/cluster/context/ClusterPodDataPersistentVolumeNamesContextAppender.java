/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpec;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.VolumeSnapshotUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterPodDataPersistentVolumeNamesContextAppender {

  private final LabelFactoryForCluster labelFactory;
  private final ResourceScanner<PersistentVolumeClaim> pvcScanner;

  public ClusterPodDataPersistentVolumeNamesContextAppender(
      LabelFactoryForCluster labelFactory,
      ResourceScanner<PersistentVolumeClaim> pvcScanner) {
    this.labelFactory = labelFactory;
    this.pvcScanner = pvcScanner;
  }

  public void appendContext(StackGresCluster cluster, List<Pod> clusterPods, Builder contextBuilder) {
    final String clusterDataPersistentVolumeClaimName =
        StackGresUtil.statefulSetPodDataPersistentVolumeClaimName(cluster);
    final List<PersistentVolumeClaim> clusterDataPvcs = getClusterDataPvcs(cluster);
    final List<Tuple2<String, PersistentVolumeClaimSpec>> podDataPvcSpecs = clusterPods
        .stream()
        .map(pod -> Tuple.tuple(
            pod.getMetadata().getName(),
            clusterDataPvcs
            .stream()
            .filter(pvc -> pvc.getMetadata().getName().equals(
                clusterDataPersistentVolumeClaimName
                + "-"
                + ResourceUtil.getIndexFromNameWithIndex(pod.getMetadata().getName())))
            .findFirst()
            .map(PersistentVolumeClaim::getSpec)))
        .filter(entry -> entry.v2.isPresent())
        .map(entry -> entry.map2(Optional::get))
        .toList();
    final Map<String, String> podDataPersistentVolumeNames = podDataPvcSpecs
        .stream()
        .map(entry -> entry.map2(PersistentVolumeClaimSpec::getVolumeName))
        .filter(entry -> entry.v2 != null)
        .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));
    contextBuilder.podDataPersistentVolumeNames(podDataPersistentVolumeNames);
    // Only the data of a persistent volume created from a volume snapshot has to be prepared to be
    // restored from it, the data of any other persistent volume was created by initdb, by a clone
    // of another instance or by a restore from the object storage and must be kept
    final Set<String> podDataPersistentVolumesFromVolumeSnapshot = podDataPvcSpecs
        .stream()
        .filter(entry -> Optional.ofNullable(entry.v2.getDataSource())
            .filter(dataSource -> VolumeSnapshotUtil.VOLUME_SNAPSHOT_GROUP.equals(dataSource.getApiGroup()))
            .filter(dataSource -> VolumeSnapshotUtil.VOLUME_SNAPSHOT_KIND.equals(dataSource.getKind()))
            .isPresent())
        .map(Tuple2::v1)
        .collect(Collectors.toSet());
    contextBuilder.podDataPersistentVolumesFromVolumeSnapshot(podDataPersistentVolumesFromVolumeSnapshot);
  }

  private List<PersistentVolumeClaim> getClusterDataPvcs(StackGresCluster cluster) {
    var clusterLabels = labelFactory.clusterLabels(cluster);
    return pvcScanner.getResourcesInNamespaceWithLabels(
        cluster.getMetadata().getNamespace(),
        clusterLabels)
        .stream()
        .filter(pod -> Optional.ofNullable(pod.getMetadata())
            .map(ObjectMeta::getDeletionTimestamp)
            .isEmpty())
        .toList();
  }

}
