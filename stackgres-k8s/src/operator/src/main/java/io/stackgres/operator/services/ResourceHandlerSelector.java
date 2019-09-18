/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.common.StackGresClusterConfig;

public interface ResourceHandlerSelector {

  boolean equals(StackGresClusterConfig config, HasMetadata existingResource, HasMetadata resource);

  HasMetadata update(StackGresClusterConfig config, HasMetadata toUpdate, HasMetadata withUpdates);

  HasMetadata create(KubernetesClient client, StackGresClusterConfig config, HasMetadata resource);

  HasMetadata patch(KubernetesClient client, StackGresClusterConfig config, HasMetadata resource);

  boolean delete(KubernetesClient client, StackGresClusterConfig config, HasMetadata resource);

  boolean isManaged(StackGresClusterConfig config, HasMetadata existingResource);

}
