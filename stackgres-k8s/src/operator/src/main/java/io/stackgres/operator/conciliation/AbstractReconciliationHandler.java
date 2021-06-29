/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.KubernetesClientFactory;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractReconciliationHandler implements ReconciliationHandler,
    ReconciliationOperations {

  private KubernetesClientFactory clientFactory;

  @Override
  public HasMetadata create(HasMetadata resource) {
    return clientFactory.withNewClient(client -> getResourceOperation(client, resource)
        .inNamespace(resource.getMetadata().getNamespace())
        .create(resource));
  }

  @Override
  public HasMetadata patch(HasMetadata resource, HasMetadata oldResource) {
    return clientFactory.withNewClient(client -> getResourceOperation(client, resource)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .patch(resource));
  }

  @Override
  public HasMetadata replace(HasMetadata resource) {
    return clientFactory.withNewClient(client -> getResourceOperation(client, resource)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .replace(resource));
  }

  @Override
  public void delete(HasMetadata resource) {
    clientFactory.withNewClient(client -> client.resource(resource).delete());
  }

  @SuppressWarnings("unchecked")
  private <M extends HasMetadata> MixedOperation<M, ? extends KubernetesResourceList<M>,
      ? extends Resource<M>> getResourceOperation(
      @NotNull KubernetesClient client, @NotNull M resource) {
    return (MixedOperation<M, ? extends KubernetesResourceList<M>, ? extends Resource<M>>)
        Optional.ofNullable(getResourceOperations(resource))
            .map(function -> function.apply(client))
            .orElseThrow(() -> new RuntimeException("Resource of type " + resource.getKind()
                + " is not configured"));
  }

  protected <M extends HasMetadata> Function<KubernetesClient,
      MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
          ? extends Resource<? extends HasMetadata>>> getResourceOperations(M resource) {
    return STACKGRES_CLUSTER_IN_NAMESPACE_RESOURCE_OPERATIONS.get(resource.getClass());
  }

  @Inject
  public void setClientFactory(KubernetesClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }
}
