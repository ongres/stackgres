/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;

public interface ResourceHandler<T extends ResourceHandlerContext> {

  boolean equals(T context, HasMetadata existingResource, HasMetadata requiredResource);

  HasMetadata update(T context, HasMetadata existingResource, HasMetadata requiredResource);

  default boolean isManaged() {
    return false;
  }

  default boolean skipCreation(T context, HasMetadata requiredResource) {
    return false;
  }

  default boolean skipUpdate(T context, HasMetadata existingResource,
      HasMetadata requiredResource) {
    return false;
  }

  default boolean skipDeletion(T context, HasMetadata existingResource) {
    return false;
  }

  default boolean isHandlerForResource(HasMetadata resource) {
    return false;
  }

  default boolean isHandlerForResource(T context, HasMetadata resource) {
    return false;
  }

  void registerKind();

  Stream<HasMetadata> getResources(@Nonnull KubernetesClient client, @Nonnull T context);

  Optional<HasMetadata> find(@Nonnull KubernetesClient client, @Nonnull HasMetadata resource);

  HasMetadata create(@Nonnull KubernetesClient client, @Nonnull HasMetadata resource);

  HasMetadata patch(@Nonnull KubernetesClient client, @Nonnull HasMetadata resource);

  boolean delete(@Nonnull KubernetesClient client, @Nonnull HasMetadata resource);
}
