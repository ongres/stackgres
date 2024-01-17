/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractUnamespacedResourceWriter<
        T extends HasMetadata, R extends Resource<T>>
    implements ResourceWriter<T> {

  private final KubernetesClient client;

  protected AbstractUnamespacedResourceWriter(KubernetesClient client) {
    this.client = client;
  }

  public AbstractUnamespacedResourceWriter() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
  }

  @Override
  public T create(T resource, boolean dryRun) {
    return getResourceEndpoints(client)
        .resource(resource)
        .dryRun(dryRun)
        .create();
  }

  @Override
  public T update(T resource, boolean dryRun) {
    return getResourceEndpoints(client)
        .resource(resource)
        .dryRun(dryRun)
        .patch();
  }

  @Override
  public T update(T resource, String patch, boolean dryRun) {
    return getResourceEndpoints(client)
        .resource(resource)
        .dryRun(dryRun)
        .patch(patch);
  }

  @Override
  public T update(@NotNull T resource, @NotNull Consumer<T> setter) {
    return KubernetesClientUtil.retryOnConflict(
        () -> {
          T resourceToUpdate = getResourceEndpoints(client)
              .withName(resource.getMetadata().getName())
              .get();
          if (resourceToUpdate == null) {
            throw new RuntimeException("Can not update resource "
                + resource.getFullResourceName()
                + " " + resource.getMetadata().getNamespace()
                + "." + resource.getMetadata().getName()
                + ": resource not found");
          }
          setter.accept(resourceToUpdate);
          return getResourceEndpoints(client)
              .resource(resourceToUpdate)
              .lockResourceVersion(resourceToUpdate.getMetadata().getResourceVersion())
              .update();
        });
  }

  @Override
  public void delete(T resource, boolean dryRun) {
    getResourceEndpoints(client)
        .resource(resource)
        .dryRun(dryRun)
        .delete();
  }

  protected abstract NonNamespaceOperation<T, ?, R> getResourceEndpoints(
      KubernetesClient client);

}
