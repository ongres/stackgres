/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterRoleFinder extends AbstractUnamespacedResourceFinderAndScanner<ClusterRole> {

  @Inject
  public ClusterRoleFinder(KubernetesClient client) {
    super(client);
  }

  @Override
  protected NonNamespaceOperation<ClusterRole,
          ? extends KubernetesResourceList<ClusterRole>, ? extends Resource<ClusterRole>>
      getOperation(KubernetesClient client) {
    return client.rbac().clusterRoles();
  }

}
