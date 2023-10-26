/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;

@ApplicationScoped
public class RoleBindingWriter extends AbstractResourceWriter<RoleBinding> {

  @Inject
  public RoleBindingWriter(KubernetesClient client) {
    super(client);
  }

  @Override
  protected MixedOperation<RoleBinding, ?, ?> getResourceEndpoints(KubernetesClient client) {
    return client.rbac().roleBindings();
  }

}
