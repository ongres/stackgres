/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;

public interface ResourceHandler<T extends ResourceHandlerContext> {

  boolean equals(T context, HasMetadata existingResource, HasMetadata requiredResource);

  HasMetadata update(T context, HasMetadata existingResource, HasMetadata requiredResource);

  default boolean isManaged() {
    return false;
  }

  default boolean skipCreation() {
    return false;
  }

  default boolean skipDeletion() {
    return false;
  }

  default boolean isHandlerForResource(HasMetadata resource) {
    return false;
  }

  default boolean isHandlerForResource(T context, HasMetadata resource) {
    return false;
  }

  void registerKind();

  Stream<HasMetadata> getResources(KubernetesClient client, T context);

  Optional<HasMetadata> find(KubernetesClient client, HasMetadata resource);

  HasMetadata create(KubernetesClient client, HasMetadata resource);

  HasMetadata patch(KubernetesClient client, HasMetadata resource);

  boolean delete(KubernetesClient client, HasMetadata resource);
}
