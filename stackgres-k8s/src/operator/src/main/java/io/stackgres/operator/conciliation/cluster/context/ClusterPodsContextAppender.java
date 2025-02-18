/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterPodsContextAppender
    extends ContextAppender<StackGresCluster, Builder> {

  private final LabelFactoryForCluster labelFactory;
  private final ResourceScanner<Pod> podScanner;
  private final ClusterPodDataPersistentVolumeNamesContextAppender clusterPodDataPersistentVolumeNamesContextAppender;

  public ClusterPodsContextAppender(
      LabelFactoryForCluster labelFactory,
      ResourceScanner<Pod> podScanner,
      ClusterPodDataPersistentVolumeNamesContextAppender clusterPodDataPersistentVolumeNamesContextAppender) {
    this.labelFactory = labelFactory;
    this.podScanner = podScanner;
    this.clusterPodDataPersistentVolumeNamesContextAppender = clusterPodDataPersistentVolumeNamesContextAppender;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final List<Pod> clusterPods = getClusterPods(cluster);
    final int currentInstances = clusterPods.size();
    contextBuilder.currentInstances(currentInstances);

    clusterPodDataPersistentVolumeNamesContextAppender.appendContext(cluster, clusterPods, contextBuilder);
  }

  private List<Pod> getClusterPods(StackGresCluster cluster) {
    var clusterLabels = labelFactory.clusterLabels(cluster);
    return podScanner.getResourcesInNamespaceWithLabels(
        cluster.getMetadata().getNamespace(),
        clusterLabels)
        .stream()
        .filter(pod -> Optional.ofNullable(pod.getMetadata())
            .map(ObjectMeta::getDeletionTimestamp)
            .isEmpty())
        .toList();
  }

}
