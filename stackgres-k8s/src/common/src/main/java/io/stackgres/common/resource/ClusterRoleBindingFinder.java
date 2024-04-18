/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterRoleBindingFinder extends AbstractUnamespacedResourceFinderAndScanner<ClusterRoleBinding> {

  @Inject
  public ClusterRoleBindingFinder(KubernetesClient client) {
    super(client);
  }

  @Override
  protected NonNamespaceOperation<ClusterRoleBinding,
          ? extends KubernetesResourceList<ClusterRoleBinding>, ? extends Resource<ClusterRoleBinding>>
      getOperation(KubernetesClient client) {
    return client.rbac().clusterRoleBindings();
  }

}
