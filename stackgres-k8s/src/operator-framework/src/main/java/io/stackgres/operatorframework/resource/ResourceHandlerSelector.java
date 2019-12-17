/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;

public interface ResourceHandlerSelector<T> {

  boolean equals(ResourceHandlerContext<T> resourceHandlerContext,
      HasMetadata existingResource, HasMetadata resource);

  HasMetadata update(ResourceHandlerContext<T> resourceHandlerContext,
      HasMetadata toUpdate, HasMetadata withUpdates);

  boolean isManaged(T config, HasMetadata existingResource);

  boolean skipCreation(T config, HasMetadata requiredResource);

  boolean skipDeletion(T config, HasMetadata requiredResource);

  void registerKinds();

  Stream<HasMetadata> getOrphanResources(KubernetesClient client,
      ImmutableList<T> existingConfigs);

  Stream<HasMetadata> getResources(KubernetesClient client, T config);

  Optional<HasMetadata> find(KubernetesClient client, T config,
      HasMetadata resource);

  HasMetadata create(KubernetesClient client, T config, HasMetadata resource);

  HasMetadata patch(KubernetesClient client, T config, HasMetadata resource);

  boolean delete(KubernetesClient client, T config, HasMetadata resource);

  ResourceHandler<T> getResourceHandler(HasMetadata resource);

  String getConfigNamespaceOf(HasMetadata resource);

  String getConfigNameOf(HasMetadata resource);

}
