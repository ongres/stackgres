/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RoleBindingFinder extends AbstractResourceFinderAndScanner<RoleBinding> {

  @Inject
  public RoleBindingFinder(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<RoleBinding, ? extends KubernetesResourceList<RoleBinding>, ? extends Resource<RoleBinding>>
      getOperation(KubernetesClient client) {
    return client.rbac().roleBindings();
  }

}
