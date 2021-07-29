/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

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
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.DeployedResourceDecorator;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;
import io.stackgres.operator.conciliation.ReconciliationOperations;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;

@ApplicationScoped
public class DbOpsDeployedResourceScanner implements DeployedResourcesScanner<StackGresDbOps>,
    ReconciliationOperations {

  private final KubernetesClientFactory clientFactory;
  private final LabelFactory<StackGresDbOps> labelFactory;
  private final Instance<DeployedResourceDecorator> decorators;

  @Inject
  public DbOpsDeployedResourceScanner(
      KubernetesClientFactory clientFactory,
      LabelFactory<StackGresDbOps> labelFactory,
      @Any Instance<DeployedResourceDecorator> decorators) {
    this.clientFactory = clientFactory;
    this.labelFactory = labelFactory;
    this.decorators = decorators;
  }

  @Override
  public List<HasMetadata> getDeployedResources(StackGresDbOps config) {
    try (KubernetesClient client = clientFactory.create()) {
      final Map<String, String> genericLabels = labelFactory.genericLabels(config);

      Stream<HasMetadata> inNamespace = IN_NAMESPACE_RESOURCE_OPERATIONS
          .values()
          .stream()
          .flatMap(resourceOperationGetter -> resourceOperationGetter.apply(client)
              .inNamespace(config.getMetadata().getNamespace())
              .withLabels(genericLabels)
              .list()
              .getItems()
              .stream());

      Stream<HasMetadata> anyNamespace = ANY_NAMESPACE_RESOURCE_OPERATIONS
          .values()
          .stream()
          .flatMap(resourceOperationGetter -> resourceOperationGetter
              .apply(client, genericLabels)
              .stream());

      List<HasMetadata> deployedResources = Stream.concat(inNamespace, anyNamespace)
          .filter(resource -> resource.getMetadata().getOwnerReferences()
              .stream().anyMatch(ownerReference -> ownerReference.getKind()
                  .equals(StackGresDbOps.KIND)
                  && ownerReference.getName().equals(config.getMetadata().getName())
                  && ownerReference.getUid().equals(config.getMetadata().getUid())))
          .collect(Collectors.toUnmodifiableList());

      deployedResources.forEach(resource -> {
        Instance<DeployedResourceDecorator> decorator = decorators
            .select(new ReconciliationScopeLiteral(StackGresDbOps.class, resource.getKind()));
        if (decorator.isResolvable()) {
          decorator.get().decorate(resource);
        }
      });

      return deployedResources;
    }
  }
}
