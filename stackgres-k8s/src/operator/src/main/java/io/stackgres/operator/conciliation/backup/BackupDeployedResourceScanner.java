/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.conciliation.DeployedResourceDecorator;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;
import io.stackgres.operator.conciliation.ReconciliationOperations;
import io.stackgres.operator.conciliation.ReconciliationScopeLiteral;

@ApplicationScoped
public class BackupDeployedResourceScanner implements DeployedResourcesScanner<StackGresBackup>,
    ReconciliationOperations {

  private final KubernetesClientFactory clientFactory;
  private final LabelFactory<StackGresBackup> labelFactory;
  private final Instance<DeployedResourceDecorator> decorators;

  @Inject
  public BackupDeployedResourceScanner(
      KubernetesClientFactory clientFactory,
      LabelFactory<StackGresBackup> labelFactory,
      @Any Instance<DeployedResourceDecorator> decorators) {
    this.clientFactory = clientFactory;
    this.labelFactory = labelFactory;
    this.decorators = decorators;
  }

  @Override
  public List<HasMetadata> getDeployedResources(StackGresBackup config) {
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

      List<HasMetadata> deployedResources = inNamespace
          .filter(resource -> resource.getMetadata().getOwnerReferences()
              .stream().anyMatch(ownerReference -> ownerReference.getKind()
                  .equals(StackGresBackup.KIND)
                  && ownerReference.getName().equals(config.getMetadata().getName())
                  && ownerReference.getUid().equals(config.getMetadata().getUid())))
          .collect(Collectors.toUnmodifiableList());

      deployedResources.forEach(resource -> {
        Instance<DeployedResourceDecorator> decorator = decorators
            .select(new ReconciliationScopeLiteral(StackGresBackup.class, resource.getKind()));
        if (decorator.isResolvable()) {
          decorator.get().decorate(resource);
        }
      });

      return deployedResources;
    }
  }

  static final ImmutableMap<
      Class<? extends HasMetadata>,
      Function<
          KubernetesClient,
          MixedOperation<
              ? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      IN_NAMESPACE_RESOURCE_OPERATIONS =
      ImmutableMap.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>builder()
          .put(Job.class, client -> client.batch().v1().jobs())
          .build();

}
