/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Namespaceable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.StackGresKubernetesClient;

public abstract class AbstractCustomResourceScheduler<T extends CustomResource<?, ?>,
    L extends CustomResourceList<T>>
    implements CustomResourceScheduler<T> {

  private final Class<T> customResourceClass;
  private final Class<L> customResourceListClass;

  @Inject
  KubernetesClient client;

  protected AbstractCustomResourceScheduler(
      KubernetesClient client,
      Class<T> customResourceClass,
      Class<L> customResourceListClass) {
    this.client = client;
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
  }

  @Override
  public T create(T resource) {
    return getCustomResourceEndpoints(client)
        .inNamespace(resource.getMetadata().getNamespace())
        .create(resource);
  }

  @Override
  public T update(T resource) {
    return getCustomResourceEndpoints(client)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .patch(resource);
  }

  @Override
  public T update(T resource, BiConsumer<T, T> setter) {
    T resourceOverwrite = getCustomResourceEndpoints(client)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .get();
    if (resourceOverwrite == null) {
      throw new RuntimeException("Can not update status of resource "
          + resource.getKind()
          + "." + resource.getGroup()
          + " " + resource.getMetadata().getNamespace()
          + "." + resource.getMetadata().getName()
          + ": resource not found");
    }
    setter.accept(resourceOverwrite, resource);
    return getCustomResourceEndpoints(client)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .lockResourceVersion(resourceOverwrite.getMetadata().getResourceVersion())
        .replace(resourceOverwrite);
  }

  @Override
  public <S> T updateStatus(T resource, Function<T, S> statusGetter,
      BiConsumer<T, S> statusSetter) {
    return ((StackGresKubernetesClient) client).updateStatus(customResourceClass,
        customResourceListClass, resource, statusGetter, statusSetter);
  }

  @Override
  public void delete(T resource) {
    getCustomResourceEndpoints(client)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .delete();
  }

  private Namespaceable<NonNamespaceOperation<T, L, Resource<T>>> getCustomResourceEndpoints(
      KubernetesClient client) {
    return client.resources(customResourceClass, customResourceListClass);
  }

}
