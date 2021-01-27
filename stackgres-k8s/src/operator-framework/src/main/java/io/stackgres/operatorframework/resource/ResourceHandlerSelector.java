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

  boolean isManaged(T config, HasMetadata existingResource);

  boolean skipCreation(T config, HasMetadata requiredResource);

  boolean skipUpdate(T config, HasMetadata existingResource, HasMetadata requiredResource);

  boolean skipDeletion(T config, HasMetadata existingResource);

  void registerKinds();

  Stream<HasMetadata> getResources(KubernetesClient client, T config);

  Optional<HasMetadata> find(KubernetesClient client, T config,
      HasMetadata resource);

  HasMetadata create(KubernetesClient client, T config, HasMetadata resource);

  HasMetadata patch(KubernetesClient client, T config, HasMetadata resource);

  boolean delete(KubernetesClient client, T config, HasMetadata resource);

  ResourceHandler<T> getResourceHandler(HasMetadata resource);

}
