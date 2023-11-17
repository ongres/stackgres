/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterRoleBindingWriter
    extends AbstractUnamespacedResourceWriter<ClusterRoleBinding, Resource<ClusterRoleBinding>> {

  @Inject
  public ClusterRoleBindingWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected NonNamespaceOperation<
          ClusterRoleBinding, ?, Resource<ClusterRoleBinding>> getResourceEndpoints(
      KubernetesClient client) {
    return client.rbac().clusterRoleBindings();
  }

}
