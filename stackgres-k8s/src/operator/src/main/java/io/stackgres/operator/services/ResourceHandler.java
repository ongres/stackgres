/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.common.StackGresClusterConfig;

public interface ResourceHandler {

  boolean equals(HasMetadata existingResource, HasMetadata requiredResource);

  HasMetadata update(HasMetadata existingResource, HasMetadata requiredResource);

  HasMetadata create(KubernetesClient client, HasMetadata resource);

  HasMetadata patch(KubernetesClient client, HasMetadata resource);

  boolean delete(KubernetesClient client, HasMetadata resource);

  default boolean handleResource(StackGresClusterConfig config, HasMetadata resource) {
    return false;
  }

  default boolean isManaged(StackGresClusterConfig config, HasMetadata existingResource) {
    return false;
  }

}
