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

public interface ResourceHandler {

  boolean equals(HasMetadata existingResource, HasMetadata requiredResource);

  HasMetadata update(HasMetadata existingResource, HasMetadata requiredResource);

  default boolean isManaged() {
    return false;
  }

  default boolean handleResource(StackGresClusterConfig config, HasMetadata resource) {
    return false;
  }

  void registerKind();

  Stream<HasMetadata> getOrphanResources(
                  KubernetesClient client, ImmutableList<StackGresClusterConfig> existingConfigs);

  Stream<HasMetadata> getResources(
                  KubernetesClient client, StackGresClusterConfig config);

  Optional<HasMetadata> find(KubernetesClient client, HasMetadata resource);

  HasMetadata create(KubernetesClient client, HasMetadata resource);

  HasMetadata patch(KubernetesClient client, HasMetadata resource);

  boolean delete(KubernetesClient client, HasMetadata resource);

}
