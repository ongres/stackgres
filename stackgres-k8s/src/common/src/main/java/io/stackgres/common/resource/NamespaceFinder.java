/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NamespaceFinder extends AbstractUnamespacedResourceFinderAndScanner<Namespace> {

  @Inject
  public NamespaceFinder(KubernetesClient client) {
    super(client);
  }

  @Override
  protected NonNamespaceOperation<Namespace,
          ? extends KubernetesResourceList<Namespace>, ? extends Resource<Namespace>>
      getOperation(KubernetesClient client) {
    return client.namespaces();
  }

}
