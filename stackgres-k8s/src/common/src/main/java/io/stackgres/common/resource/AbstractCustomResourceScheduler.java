/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Namespaceable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCustomResourceScheduler<T extends CustomResource<?, ?>,
    L extends DefaultKubernetesResourceList<T>>
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
        .resource(resource)
        .create();
  }

  @Override
  public T update(T resource) {
    return getCustomResourceEndpoints()
        .inNamespace(resource.getMetadata().getNamespace())
        .resource(resource)
        .patch();
  }

  @Override
  public T update(T resource, Consumer<T> setter) {
    return KubernetesClientUtil.retryOnConflict(
        () -> {
          T resourceToUpdate = getCustomResourceEndpoints()
              .inNamespace(resource.getMetadata().getNamespace())
              .withName(resource.getMetadata().getName())
              .get();
          if (resourceToUpdate == null) {
            throw new RuntimeException("Can not update resource "
                + resource.getKind()
                + "." + resource.getGroup()
                + " " + resource.getMetadata().getNamespace()
                + "." + resource.getMetadata().getName()
                + ": resource not found");
          }
          setter.accept(resourceToUpdate);
          return getCustomResourceEndpoints()
              .inNamespace(resource.getMetadata().getNamespace())
              .resource(resourceToUpdate)
              .lockResourceVersion(resourceToUpdate.getMetadata().getResourceVersion())
              .update();
        });
  }

  @Override
  public <S> T updateStatus(T resource, Consumer<T> setter) {
    return KubernetesClientUtil.retryOnConflict(
        () -> {
          var resourceToUpdate = getCustomResourceEndpoints()
              .inNamespace(resource.getMetadata().getNamespace())
              .withName(resource.getMetadata().getName())
              .get();
          setter.accept(resourceToUpdate);
          return getCustomResourceEndpoints()
              .inNamespace(resource.getMetadata().getNamespace())
              .resource(resourceToUpdate)
              .lockResourceVersion(resourceToUpdate.getMetadata().getResourceVersion())
              .updateStatus();
        });
  }

  @Override
  public void delete(T resource) {
    getCustomResourceEndpoints()
        .inNamespace(resource.getMetadata().getNamespace())
        .resource(resource)
        .delete();
  }

  private Namespaceable<NonNamespaceOperation<T, L, Resource<T>>> getCustomResourceEndpoints() {
    return client.resources(customResourceClass, customResourceListClass);
  }

}
