/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.DeployedResourceDecorator;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;
import io.stackgres.operator.conciliation.ReconciliationOperations;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;

@ApplicationScoped
public class ClusterDeployedResourceScanner implements DeployedResourcesScanner<StackGresCluster>,
    ReconciliationOperations {

  private final KubernetesClient client;
  private final LabelFactoryForCluster<StackGresCluster> labelFactory;
  private final Instance<DeployedResourceDecorator> decorators;

  @Inject
  public ClusterDeployedResourceScanner(
      KubernetesClient client,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      @Any Instance<DeployedResourceDecorator> decorators) {
    this.client = client;
    this.labelFactory = labelFactory;
    this.decorators = decorators;
  }

  @Override
  public List<HasMetadata> getDeployedResources(StackGresCluster config) {
    final Map<String, String> genericClusterLabels = labelFactory.genericLabels(config);

    StackGresKubernetesClient stackGresClient = (StackGresKubernetesClient) client;

    Stream<HasMetadata> inNamespace = MANAGED_RESOURCE_OPERATIONS
        .stream().flatMap(clazz -> stackGresClient.findManagedIntents(
                clazz,
                ResourceWriter.STACKGRES_FIELD_MANAGER,
                genericClusterLabels,
                config.getMetadata().getNamespace()
            ).stream()
        );

    Stream<HasMetadata> anyNamespace = ANY_NAMESPACE_RESOURCE_OPERATIONS
        .values()
        .stream()
        .flatMap(resourceOperationGetter -> resourceOperationGetter
            .apply(client, genericClusterLabels)
            .stream());

    List<HasMetadata> deployedResources = Stream.concat(inNamespace, anyNamespace)
        .filter(resource1 -> resource1.getMetadata().getOwnerReferences()
            .stream().anyMatch(ownerReference -> ownerReference.getKind()
                .equals(StackGresCluster.KIND)
                && ownerReference.getName().equals(config.getMetadata().getName())
                && ownerReference.getUid().equals(config.getMetadata().getUid())))
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
