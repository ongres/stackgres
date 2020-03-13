/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.transformer.ClusterTransformer;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterDtoScanner implements CustomResourceScanner<ClusterDto> {

  private final CustomResourceScanner<StackGresCluster> clusterScanner;
  private final KubernetesClientFactory clientFactory;
  private final ClusterTransformer clusterTransformer;

  @Inject
  public ClusterDtoScanner(CustomResourceScanner<StackGresCluster> clusterScanner,
      KubernetesClientFactory clientFactory,
      ClusterTransformer clusterTransformer) {
    this.clusterScanner = clusterScanner;
    this.clientFactory = clientFactory;
    this.clusterTransformer = clusterTransformer;
  }

  @Override
  public List<ClusterDto> getResources() {
    Transformer transformer = createTransformer();
    return Seq.seq(clusterScanner.getResources())
        .map(transformer::transform)
        .toList();
  }

  @Override
  public List<ClusterDto> getResources(String namespace) {
    Transformer transformer = createTransformer();
    return Seq.seq(clusterScanner.getResources(namespace))
        .map(transformer::transform)
        .toList();
  }

  @Override
  public Optional<List<ClusterDto>> findResources() {
    Transformer transformer = createTransformer();
    return clusterScanner.findResources()
        .map(resources -> Seq.seq(resources)
            .map(transformer::transform)
            .toList());
  }

  @Override
  public Optional<List<ClusterDto>> findResources(String namespace) {
    Transformer transformer = createTransformer();
    return clusterScanner.findResources(namespace)
        .map(resources -> Seq.seq(resources)
            .map(transformer::transform)
            .toList());
  }

  private Transformer createTransformer() {
    try (KubernetesClient client = clientFactory.create()) {
      return new Transformer(Seq.seq(getAllClusterPods(client))
          .groupBy(pod -> pod.getMetadata().getLabels().get(StackGresUtil.CLUSTER_UID_KEY)));
    }
  }

  private class Transformer {
    private final Map<String, List<Pod>> clusterPodsMap;

    public Transformer(Map<String, List<Pod>> clusterPodsMap) {
      this.clusterPodsMap = clusterPodsMap;
    }

    private ClusterDto transform(StackGresCluster cluster) {
      return clusterTransformer.toResourceWithPods(cluster,
          clusterPodsMap.get(cluster.getMetadata().getUid()));
    }
  }

  private List<Pod> getAllClusterPods(KubernetesClient client) {
    return client.pods()
        .inAnyNamespace()
        .withLabels(StackGresUtil.patroniClusterLabels())
        .list()
        .getItems();
  }

}
