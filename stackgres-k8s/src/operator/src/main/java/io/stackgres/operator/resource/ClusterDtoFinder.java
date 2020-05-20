/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.StackGresUserClusterContext;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.transformer.ClusterTransformer;

@ApplicationScoped
public class ClusterDtoFinder implements CustomResourceFinder<ClusterDto> {

  private CustomResourceFinder<StackGresCluster> clusterFinder;
  private KubernetesClientFactory clientFactory;
  private ClusterTransformer clusterTransformer;

  @Override
  public Optional<ClusterDto> findByNameAndNamespace(String name, String namespace) {
    return clusterFinder.findByNameAndNamespace(name, namespace).map(cluster -> {
      try (KubernetesClient client = clientFactory.create()) {
        return clusterTransformer.toResourceWithPods(cluster,
            getClusterPods(cluster, client));
      }
    });
  }

  private List<Pod> getClusterPods(StackGresCluster cluster, KubernetesClient client) {
    return client.pods()
        .inNamespace(cluster.getMetadata().getNamespace())
        .withLabels(StackGresUserClusterContext.getClusterLabelMapper(cluster)
            .patroniClusterLabels())
        .list()
        .getItems();
  }

  @Inject
  public void setClusterFinder(CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Inject
  public void setClientFactory(KubernetesClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @Inject
  public void setClusterTransformer(ClusterTransformer clusterTransformer) {
    this.clusterTransformer = clusterTransformer;
  }

}
