/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterRoleWriter
    extends AbstractUnamespacedResourceWriter<ClusterRole, Resource<ClusterRole>> {

  @Inject
  public ClusterRoleWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected NonNamespaceOperation<
          ClusterRole, ?, Resource<ClusterRole>> getResourceEndpoints(
      KubernetesClient client) {
    return client.rbac().clusterRoles();
  }

}
