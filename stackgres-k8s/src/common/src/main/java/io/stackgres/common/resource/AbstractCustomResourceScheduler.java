/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Namespaceable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.stackgres.common.KubernetesClientFactory;

public abstract class AbstractCustomResourceScheduler<T extends CustomResource,
    L extends CustomResourceList<T>, D extends CustomResourceDoneable<T>>
    implements CustomResourceScheduler<T> {

  private final KubernetesClientFactory clientFactory;

  private final String customResourceName;
  private final Class<T> customResourceClass;
  private final Class<L> customResourceListClass;
  private final Class<D> customResourceDoneClass;

  protected AbstractCustomResourceScheduler(
      KubernetesClientFactory clientFactory,
      String customResourceName,
      Class<T> customResourceClass,
      Class<L> customResourceListClass,
      Class<D> customResourceDoneClass) {
    this.clientFactory = clientFactory;
    this.customResourceName = customResourceName;
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
    this.customResourceDoneClass = customResourceDoneClass;
  }

  @Override
  public void create(T resource) {
    try (KubernetesClient client = clientFactory.create()) {
      getCustomResourceEndpoints(client)
          .inNamespace(resource.getMetadata().getNamespace())
          .create(resource);
    }
  }

  @Override
  public void update(T resource) {
    try (KubernetesClient client = clientFactory.create()) {
      getCustomResourceEndpoints(client)
          .inNamespace(resource.getMetadata().getNamespace())
          .withName(resource.getMetadata().getName())
          .patch(resource);
    }
  }

  @Override
  public void delete(T resource) {
    try (KubernetesClient client = clientFactory.create()) {
      getCustomResourceEndpoints(client)
          .inNamespace(resource.getMetadata().getNamespace())
          .withName(resource.getMetadata().getName())
          .withPropagationPolicy("Background")
          .delete();
    }
  }

  private Namespaceable<NonNamespaceOperation<T, L, D, Resource<T, D>>> getCustomResourceEndpoints(
      KubernetesClient client) {
    CustomResourceDefinitionContext crd = ResourceUtil.getCustomResource(
        client, customResourceName)
        .map(CustomResourceDefinitionContext::fromCrd)
        .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
            + " CRD " + customResourceName + " not found."));

    return client.customResources(crd,
        customResourceClass,
        customResourceListClass,
        customResourceDoneClass);
  }

}
