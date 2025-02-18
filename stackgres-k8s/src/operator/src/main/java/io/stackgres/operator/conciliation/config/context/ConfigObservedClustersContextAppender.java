/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterObservability;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.common.ObservedClusterContext;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.config.StackGresConfigContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigObservedClustersContextAppender
    extends ContextAppender<StackGresConfig, Builder> {

  private final CustomResourceScanner<StackGresCluster> clusterScanner;

  private final LabelFactoryForCluster labelFactoryForCluster;

  private final ResourceScanner<Pod> podScanner;

  public ConfigObservedClustersContextAppender(
      CustomResourceScanner<StackGresCluster> clusterScanner,
      LabelFactoryForCluster labelFactoryForCluster, ResourceScanner<Pod> podScanner) {
    this.clusterScanner = clusterScanner;
    this.labelFactoryForCluster = labelFactoryForCluster;
    this.podScanner = podScanner;
  }

  public void appendContext(StackGresConfig config, Builder contextBuilder) {
    final List<ObservedClusterContext> observerdClusters = clusterScanner.getResources()
        .stream()
        .filter(cluster -> Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getConfigurations)
            .map(StackGresClusterConfigurations::getObservability)
            .map(StackGresClusterObservability::getPrometheusAutobind)
            .orElse(false))
        .map(cluster -> ObservedClusterContext.toObservedClusterContext(
            cluster,
            podScanner.getResourcesInNamespaceWithLabels(
                cluster.getMetadata().getNamespace(),
                labelFactoryForCluster.clusterLabels(cluster))))
        .toList();
    contextBuilder.observedClusters(observerdClusters);
  }

}
