/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StorageClassFinder extends AbstractUnamespacedResourceFinderAndScanner<StorageClass> {

  @Inject
  public StorageClassFinder(KubernetesClient client) {
    super(client);
  }

  @Override
  protected NonNamespaceOperation<StorageClass,
          ? extends KubernetesResourceList<StorageClass>, ? extends Resource<StorageClass>>
      getOperation(KubernetesClient client) {
    return client.storage().v1().storageClasses();
  }

}
