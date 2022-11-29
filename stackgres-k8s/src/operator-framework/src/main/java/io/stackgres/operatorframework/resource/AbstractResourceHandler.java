/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.operatorframework.resource.visitor.ResourcePairVisitor;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractResourceHandler<T extends ResourceHandlerContext>
    implements ResourceHandler<T> {

  @Override
  public boolean equals(T context, HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.equals(context, existingResource, requiredResource);
  }

  @Override
  public HasMetadata update(T context, HasMetadata existingResource, HasMetadata requiredResource) {
    return ResourcePairVisitor.update(context, existingResource, requiredResource);
  }

  @Override
  public void registerKind() {}

  @Override
  public Stream<HasMetadata> getResources(@NotNull KubernetesClient client,  @NotNull T context) {
    return Stream.empty();
  }

  @Override
  public Optional<HasMetadata> find(@NotNull KubernetesClient client,
      @NotNull HasMetadata resource) {
    return Optional.ofNullable(getResourceOperation(client, resource)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .get());
  }

  @Override
  public HasMetadata create(@NotNull KubernetesClient client, @NotNull HasMetadata resource) {
    return getResourceOperation(client, resource)
        .inNamespace(resource.getMetadata().getNamespace())
        .create(resource);
  }

  @Override
  public HasMetadata patch(@NotNull KubernetesClient client, @NotNull HasMetadata resource) {
    return getResourceOperation(client, resource)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .patch(resource);
  }

  @Override
  public boolean delete(KubernetesClient client, HasMetadata resource) {
    return client.resource(resource).delete().isEmpty();
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

  protected abstract <M extends HasMetadata> Function<KubernetesClient,
      MixedOperation<? extends HasMetadata,
      ? extends KubernetesResourceList<? extends HasMetadata>,
      ? extends Resource<? extends HasMetadata>>> getResourceOperations(
      M resource);

}
