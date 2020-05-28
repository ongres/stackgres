/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.apiweb.distributedlogs.dto.cluster.ClusterDto;
import io.stackgres.apiweb.transformer.ClusterTransformer;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;

@ApplicationScoped
public class ClusterDtoFinder implements CustomResourceFinder<ClusterDto> {

  private CustomResourceFinder<StackGresCluster> clusterFinder;
  private KubernetesClientFactory clientFactory;
  private ClusterTransformer clusterTransformer;
  private LabelFactory<StackGresCluster> labelFactory;

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
        .withLabels(getPatroniClusterLabels(cluster))
        .list()
        .getItems();
  }

  private Map<String, String> getPatroniClusterLabels(StackGresCluster cluster) {
    return labelFactory.patroniClusterLabels(cluster);
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

  @Inject
  public void setLabelFactory(LabelFactory<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
