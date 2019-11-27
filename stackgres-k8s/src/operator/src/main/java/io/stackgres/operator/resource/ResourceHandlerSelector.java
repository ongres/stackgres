/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.controller.ResourceHandlerContext;

public interface ResourceHandlerSelector {

  boolean equals(ResourceHandlerContext resourceHandlerContext,
      HasMetadata existingResource, HasMetadata resource);

  HasMetadata update(ResourceHandlerContext resourceHandlerContext,
      HasMetadata toUpdate, HasMetadata withUpdates);

  boolean isManaged(StackGresClusterConfig config, HasMetadata existingResource);

  boolean skipCreation(StackGresClusterConfig config, HasMetadata requiredResource);

  boolean skipDeletion(StackGresClusterConfig config, HasMetadata requiredResource);

  void registerKinds();

  Stream<HasMetadata> getOrphanResources(KubernetesClient client,
      ImmutableList<StackGresClusterConfig> existingConfigs);

  Stream<HasMetadata> getResources(KubernetesClient client, StackGresClusterConfig config);

  Optional<HasMetadata> find(KubernetesClient client, StackGresClusterConfig config,
      HasMetadata resource);

  HasMetadata create(KubernetesClient client, StackGresClusterConfig config, HasMetadata resource);

  HasMetadata patch(KubernetesClient client, StackGresClusterConfig config, HasMetadata resource);

  boolean delete(KubernetesClient client, StackGresClusterConfig config, HasMetadata resource);

  ResourceHandler getResourceHandler(HasMetadata resource);

}
