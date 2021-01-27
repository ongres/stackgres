/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;

public interface ResourceHandlerSelector<T extends ResourceHandlerContext> {

  boolean equals(T resourceHandlerContext,
      HasMetadata existingResource, HasMetadata resource);

  HasMetadata update(T resourceHandlerContext,
      HasMetadata toUpdate, HasMetadata withUpdates);

  boolean isManaged(T context, HasMetadata existingResource);

  boolean skipCreation(T context, HasMetadata requiredResource);

  boolean skipUpdate(T config, HasMetadata existingResource, HasMetadata requiredResource);

  boolean skipDeletion(T context, HasMetadata existingResource);

  void registerKinds();

  Stream<HasMetadata> getResources(KubernetesClient client, T context);

  Optional<HasMetadata> find(KubernetesClient client, T context,
      HasMetadata resource);

  HasMetadata create(KubernetesClient client, T context, HasMetadata resource);

  HasMetadata patch(KubernetesClient client, T context, HasMetadata resource);

  boolean delete(KubernetesClient client, T context, HasMetadata resource);

  ResourceHandler<T> getResourceHandler(HasMetadata resource);

}
