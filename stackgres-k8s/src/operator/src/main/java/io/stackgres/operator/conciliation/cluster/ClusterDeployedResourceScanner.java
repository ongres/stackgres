/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.DeployedResourceDecorator;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;
import io.stackgres.operator.conciliation.ReconciliationOperations;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;

@ApplicationScoped
public class ClusterDeployedResourceScanner implements DeployedResourcesScanner<StackGresCluster>,
    ReconciliationOperations {

  private final KubernetesClientFactory clientFactory;
  private final LabelFactory<StackGresCluster> labelFactory;
  private final Instance<DeployedResourceDecorator> decorators;

  @Inject
  public ClusterDeployedResourceScanner(
      KubernetesClientFactory clientFactory,
      LabelFactory<StackGresCluster> labelFactory,
      @Any Instance<DeployedResourceDecorator> decorators) {
    this.clientFactory = clientFactory;
    this.labelFactory = labelFactory;
    this.decorators = decorators;
  }

  @Override
  public List<HasMetadata> getDeployedResources(StackGresCluster config) {

    try (KubernetesClient client = clientFactory.create()) {

      List<HasMetadata> deployedResources = STACKGRES_CLUSTER_RESOURCE_OPERATIONS.values()
          .stream()
          .flatMap(resourceOperationGetter -> resourceOperationGetter.apply(client)
              .inNamespace(config.getMetadata().getNamespace())
              .withLabels(labelFactory.genericClusterLabels(config))
              .list()
              .getItems()
              .stream())
          .filter(resource -> resource.getMetadata().getOwnerReferences()
              .stream().anyMatch(ownerReference -> ownerReference.getKind()
                  .equals(StackGresCluster.KIND)
                  && ownerReference.getName().equals(config.getMetadata().getName())))
          .collect(Collectors.toUnmodifiableList());

      deployedResources.forEach(resource -> {
        Instance<DeployedResourceDecorator> decorator = decorators
            .select(new ReconciliationScopeLiteral(StackGresCluster.class, resource.getKind()));
        if (decorator.isResolvable()) {
          decorator.get().decorate(resource);
        }
      });

      return deployedResources;
    }

  }
}
