/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;

public class EmptyResourceHandlerSelector<T extends ResourceHandlerContext>
    implements ResourceHandlerSelector<T> {

  @Override
  public boolean equals(T resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HasMetadata update(T resourceHandlerContext,
      HasMetadata existingResource, HasMetadata requiredResource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isManaged(T context, HasMetadata existingResource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean skipCreation(T context, HasMetadata requiredResource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean skipUpdate(T config, HasMetadata existingResource, HasMetadata requiredResource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean skipDeletion(T context, HasMetadata requiredResource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client, T context) {
    return Stream.of();
  }

  @Override
  public Optional<HasMetadata> find(KubernetesClient client, T context, HasMetadata resource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HasMetadata create(KubernetesClient client, T context, HasMetadata resource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HasMetadata patch(KubernetesClient client, T context, HasMetadata resource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean delete(KubernetesClient client, T context, HasMetadata resource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResourceHandler<T> getResourceHandler(HasMetadata resource) {
    throw new UnsupportedOperationException();
  }

  protected Stream<ResourceHandler<T>> getResourceHandlers() {
    return Stream.of();
  }

  protected Optional<ResourceHandler<T>> getDefaultResourceHandler() {
    return Optional.empty();
  }

}
