/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.transformer.ClusterTransformer;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.PodFinder;

@ApplicationScoped
public class ClusterDtoFinder implements CustomResourceFinder<ClusterDto> {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final PodFinder podFinder;
  private final ClusterTransformer clusterTransformer;
  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  public ClusterDtoFinder(CustomResourceFinder<StackGresCluster> clusterFinder, PodFinder podFinder,
      ClusterTransformer clusterTransformer, LabelFactory<StackGresCluster> labelFactory) {
    super();
    this.clusterFinder = clusterFinder;
    this.podFinder = podFinder;
    this.clusterTransformer = clusterTransformer;
    this.labelFactory = labelFactory;
  }

  @Override
  public Optional<ClusterDto> findByNameAndNamespace(String name, String namespace) {
    return clusterFinder.findByNameAndNamespace(name, namespace)
        .map(cluster -> clusterTransformer.toResourceWithPods(cluster,
            getClusterPods(cluster)));
  }

  private List<Pod> getClusterPods(StackGresCluster cluster) {
    return podFinder.findResourcesInNamespaceWithLabels(
        cluster.getMetadata().getNamespace(),
        labelFactory.patroniClusterLabels(cluster));
  }

}
