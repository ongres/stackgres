/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.transformer.ClusterTransformer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.PodFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterDtoScanner implements CustomResourceScanner<ClusterDto> {

  private CustomResourceScanner<StackGresCluster> clusterScanner;
  private PodFinder podFinder;
  private ClusterTransformer clusterTransformer;
  private LabelFactoryForCluster labelFactory;

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
  public @NotNull List<@NotNull ClusterDto> getResourcesWithLabels(Map<String, String> labels) {
    Transformer transformer = createTransformer();
    return Seq.seq(clusterScanner.getResourcesWithLabels(labels))
        .map(transformer::transform)
        .toList();
  }

  @Override
  public @NotNull List<@NotNull ClusterDto> getResourcesWithLabels(
      String namespace, Map<String, String> labels) {
    Transformer transformer = createTransformer();
    return Seq.seq(clusterScanner.getResourcesWithLabels(namespace, labels))
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
    return new Transformer(getAllClusterPods());
  }

  private class Transformer {
    private final List<Pod> clusterPods;

    public Transformer(List<Pod> clusterPods) {
      this.clusterPods = clusterPods;
    }

    ClusterDto transform(StackGresCluster cluster) {
      var clusterLabels = labelFactory.clusterLabels(cluster);
      return clusterTransformer.toResourceWithPods(cluster,
          clusterPods.stream()
          .filter(pod -> pod.getMetadata().getLabels() != null
              && clusterLabels.entrySet().stream()
              .allMatch(clusterLabel -> pod.getMetadata().getLabels().entrySet().stream()
                  .anyMatch(clusterLabel::equals)))
          .toList());
    }
  }

  private List<Pod> getAllClusterPods() {
    return podFinder.getResourcesWithLabels(labelFactory.appLabel());
  }

  @Inject
  public void setClusterScanner(CustomResourceScanner<StackGresCluster> clusterScanner) {
    this.clusterScanner = clusterScanner;
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
  public void setLabelFactory(LabelFactoryForCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

}
