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
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCustomResourceScheduler<T extends CustomResource<?, ?>,
    L extends CustomResourceList<T>>
    implements CustomResourceScheduler<T> {

  @NotNull
  private final Class<T> customResourceClass;
  @NotNull
  private final Class<L> customResourceListClass;

  @Inject
  KubernetesClient client;

  protected AbstractCustomResourceScheduler(
      @NotNull Class<T> customResourceClass,
      @NotNull Class<L> customResourceListClass) {
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
  }

  @Override
  public T create(T resource) {
    return getCustomResourceEndpoints()
        .inNamespace(resource.getMetadata().getNamespace())
        .create(resource);
  }

  @Override
  public T update(T resource) {
    return getCustomResourceEndpoints()
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .patch(resource);
  }

  @Override
  public T update(T resource, BiConsumer<T, T> setter) {
    return KubernetesClientUtil.retryOnConflict(
        () -> {
          T resourceOverwrite = getCustomResourceEndpoints()
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
          return getCustomResourceEndpoints()
              .inNamespace(resource.getMetadata().getNamespace())
              .withName(resource.getMetadata().getName())
              .lockResourceVersion(resourceOverwrite.getMetadata().getResourceVersion())
              .replace(resourceOverwrite);
        });
  }

  @Override
  public <S> T updateStatus(T resource, Function<T, S> statusGetter,
      BiConsumer<T, S> statusSetter) {
    return KubernetesClientUtil.retryOnConflict(
        () -> ((StackGresKubernetesClient) client).updateStatus(customResourceClass,
            customResourceListClass, resource, statusGetter, statusSetter));
  }

  @Override
  public void delete(T resource) {
    getCustomResourceEndpoints()
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .delete();
  }

  private Namespaceable<NonNamespaceOperation<T, L, Resource<T>>> getCustomResourceEndpoints() {
    return client.resources(customResourceClass, customResourceListClass);
  }

}
