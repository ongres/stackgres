/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.transformer.ClusterTransformer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.PodFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterDtoFinder implements CustomResourceFinder<ClusterDto> {

  private CustomResourceFinder<StackGresCluster> clusterFinder;
  private PodFinder podFinder;
  private ClusterTransformer clusterTransformer;
  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Override
  public Optional<ClusterDto> findByNameAndNamespace(String name, String namespace) {
    return clusterFinder.findByNameAndNamespace(name, namespace)
        .map(cluster -> clusterTransformer.toResourceWithPods(cluster,
            getClusterPods(cluster)));
  }

  private List<Pod> getClusterPods(StackGresCluster cluster) {
    return podFinder.getResourcesInNamespaceWithLabels(
        cluster.getMetadata().getNamespace(),
        labelFactory.clusterLabels(cluster));
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
  public void setClusterTransformer(ClusterTransformer clusterTransformer) {
    this.clusterTransformer = clusterTransformer;
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
