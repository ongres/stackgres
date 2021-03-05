/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jetbrains.annotations.NotNull;

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

  Stream<HasMetadata> getResources(@NotNull KubernetesClient client, @NotNull T context);

  Optional<HasMetadata> find(@NotNull KubernetesClient client, @NotNull HasMetadata resource);

  HasMetadata create(@NotNull KubernetesClient client, @NotNull HasMetadata resource);

  HasMetadata patch(@NotNull KubernetesClient client, @NotNull HasMetadata resource);

  boolean delete(@NotNull KubernetesClient client, @NotNull HasMetadata resource);
}
