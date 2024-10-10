/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.FileSystemHandler;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.extension.DocirExtensionManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterDocirExtensionManager extends DocirExtensionManager {

  @Inject
  public ClusterDocirExtensionManager(
      StackGresContext context,
      KubernetesClient client,
      ClusterControllerPropertyContext propertyContext) {
    super(
        context,
        client,
        propertyContext.get(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME).get(),
        new FileSystemHandler(),
        !propertyContext.getBoolean(
            ClusterControllerProperty.CLUSTER_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES));
  }

}
