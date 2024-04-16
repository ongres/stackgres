/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Namespaceable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import jakarta.inject.Inject;

public abstract class AbstractCustomResourceScheduler<T extends CustomResource<?, ?>,
    L extends DefaultKubernetesResourceList<T>>
    implements CustomResourceScheduler<T> {

  @Nonnull
  private final Class<T> customResourceClass;
  @Nonnull
  private final Class<L> customResourceListClass;

  @Inject
  KubernetesClient client;

  protected AbstractCustomResourceScheduler(
      @Nonnull Class<T> customResourceClass,
      @Nonnull Class<L> customResourceListClass) {
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
  }

  @Override
  public T create(T resource, boolean dryRun) {
    return getCustomResourceEndpoints()
        .inNamespace(resource.getMetadata().getNamespace())
        .resource(resource)
        .dryRun(dryRun)
        .create();
  }

  @Override
  public T update(T resource, boolean dryRun) {
    return getCustomResourceEndpoints()
        .inNamespace(resource.getMetadata().getNamespace())
        .resource(resource)
        .dryRun(dryRun)
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
  public void delete(T resource, boolean dryRun) {
    getCustomResourceEndpoints()
        .inNamespace(resource.getMetadata().getNamespace())
        .resource(resource)
        .dryRun(dryRun)
        .delete();
  }

  private Namespaceable<NonNamespaceOperation<T, L, Resource<T>>> getCustomResourceEndpoints() {
    return client.resources(customResourceClass, customResourceListClass);
  }

}
