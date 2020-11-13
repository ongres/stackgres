/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Namespaceable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.KubernetesClientFactory;

public abstract class AbstractResourceWriter<T extends HasMetadata,
    L extends KubernetesResourceList<T>, D extends Doneable<T>>
    implements ResourceWriter<T> {

  private final KubernetesClientFactory clientFactory;

  protected AbstractResourceWriter(KubernetesClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @Override
  public void create(T resource) {
    try (KubernetesClient client = clientFactory.create()) {
      getResourceEndpoints(client)
          .inNamespace(resource.getMetadata().getNamespace())
          .create(resource);
    }
  }

  @Override
  public void update(T resource) {
    try (KubernetesClient client = clientFactory.create()) {
      getResourceEndpoints(client)
          .inNamespace(resource.getMetadata().getNamespace())
          .withName(resource.getMetadata().getName())
          .patch(resource);
    }
  }

  @Override
  public void delete(T resource) {
    try (KubernetesClient client = clientFactory.create()) {
      getResourceEndpoints(client)
          .inNamespace(resource.getMetadata().getNamespace())
          .withName(resource.getMetadata().getName())
          .delete();
    }
  }

  protected abstract Namespaceable<
      NonNamespaceOperation<T, L, D, Resource<T, D>>> getResourceEndpoints(
      KubernetesClient client);

}
