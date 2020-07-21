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
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.transformer.ClusterTransformer;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.PodFinder;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterDtoScanner implements CustomResourceScanner<ClusterDto> {

  private final CustomResourceScanner<StackGresCluster> clusterScanner;
  private final PodFinder podFinder;
  private final ClusterTransformer clusterTransformer;
  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  public ClusterDtoScanner(CustomResourceScanner<StackGresCluster> clusterScanner,
      PodFinder podFinder, ClusterTransformer clusterTransformer,
      LabelFactory<StackGresCluster> labelFactory) {
    super();
    this.clusterScanner = clusterScanner;
    this.podFinder = podFinder;
    this.clusterTransformer = clusterTransformer;
    this.labelFactory = labelFactory;
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
    return new Transformer(Seq.seq(getAllClusterPods())
        .groupBy(pod -> pod.getMetadata().getLabels().get(StackGresContext.CLUSTER_UID_KEY)));
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

  private List<Pod> getAllClusterPods() {
    return podFinder.findResourcesWithLabels(labelFactory.anyPatroniClusterLabels());
  }

}
