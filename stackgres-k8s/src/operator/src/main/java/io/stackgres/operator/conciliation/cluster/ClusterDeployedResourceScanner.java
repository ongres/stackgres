/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;
import io.stackgres.operator.conciliation.ReconciliationOperations;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterDeployedResourceScanner extends DeployedResourcesScanner<StackGresCluster>
    implements ReconciliationOperations {

  private final KubernetesClient client;
  private final LabelFactoryForCluster<StackGresCluster> labelFactory;
  private final boolean prometheusAutobind;

  @Inject
  public ClusterDeployedResourceScanner(
      KubernetesClient client,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      OperatorPropertyContext operatorContext) {
    this.client = client;
    this.labelFactory = labelFactory;
    this.prometheusAutobind = operatorContext.getBoolean(OperatorProperty.PROMETHEUS_AUTOBIND);
  }

  @Override
  protected Map<String, String> getGenericLabels(StackGresCluster config) {
    return labelFactory.genericLabels(config);
  }

  @Override
  protected Map<String, String> getCrossNamespaceLabels(StackGresCluster config) {
    return labelFactory.clusterCrossNamespaceLabels(config);
  }

  @Override
  protected KubernetesClient getClient() {
    return client;
  }

  @Override
  protected Map<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>> getInNamepspaceResourceOperations() {
    return IN_NAMESPACE_RESOURCE_OPERATIONS;
  }

  @Override
  protected Map<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>> getInAnyNamespaceResourceOperations(
                  StackGresCluster cluster) {
    if (prometheusAutobind && Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPrometheusAutobind)
        .orElse(false)) {
      return PROMETHEUS_RESOURCE_OPERATIONS;
    }
    return super.getInAnyNamespaceResourceOperations(cluster);
  }

}
