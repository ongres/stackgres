/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static io.stackgres.common.resource.ResourceWriter.STACKGRES_FIELD_MANAGER;

import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.stackgres.common.StackGresKubernetesClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReconciliationHandler<T extends CustomResource<?, ?>>
    implements ReconciliationHandler<T>, ReconciliationOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger("io.stackgres.reconciliator");

  private KubernetesClient client;

  @Override
  public HasMetadata create(T context, HasMetadata resource) {
    return ((StackGresKubernetesClient) client).serverSideApply(new PatchContext.Builder()
        .withFieldManager(STACKGRES_FIELD_MANAGER)
        .withForce(true)
        .build(), resource);
  }

  @Override
  public HasMetadata patch(T context, HasMetadata resource, HasMetadata oldResource) {
    try {
      return ((StackGresKubernetesClient) client).serverSideApply(new PatchContext.Builder()
          .withFieldManager(STACKGRES_FIELD_MANAGER)
          .withForce(true)
          .build(), resource);
    } catch (KubernetesClientException ex) {
      LOGGER.warn("Server side apply failed, switching back to JSON merge", ex);
      return getResourceOperation(client, resource)
          .inNamespace(resource.getMetadata().getNamespace())
          .withName(resource.getMetadata().getName())
          .patch(resource);
    }
  }

  @Override
  public HasMetadata replace(T context, HasMetadata resource) {
    return getResourceOperation(client, resource)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .replace(resource);
  }

  @Override
  public void delete(T context, HasMetadata resource) {
    client.resource(resource).delete();
  }

  @SuppressWarnings("unchecked")
  private <M extends HasMetadata> MixedOperation<M, ? extends KubernetesResourceList<M>,
      ? extends Resource<M>> getResourceOperation(
      @NotNull KubernetesClient client, @NotNull M resource) {
    return (MixedOperation<M, ? extends KubernetesResourceList<M>, ? extends Resource<M>>) Optional
        .ofNullable(getResourceOperations(resource))
        .map(function -> function.apply(client))
        .orElseThrow(() -> new RuntimeException("Resource of type " + resource.getKind()
            + " is not configured"));
  }

  protected <M extends HasMetadata> Function<KubernetesClient, MixedOperation<? extends HasMetadata,
      ? extends KubernetesResourceList<? extends HasMetadata>,
      ? extends Resource<? extends HasMetadata>>> getResourceOperations(M resource) {
    return IN_NAMESPACE_RESOURCE_OPERATIONS.get(resource.getClass());
  }

  @Inject
  public void setClient(KubernetesClient client) {
    this.client = client;
  }
}
