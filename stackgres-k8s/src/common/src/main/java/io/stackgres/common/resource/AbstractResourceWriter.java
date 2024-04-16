/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;

public abstract class AbstractResourceWriter<T extends HasMetadata>
    implements ResourceWriter<T> {

  private final KubernetesClient client;

  protected AbstractResourceWriter(KubernetesClient client) {
    this.client = client;
  }

  public AbstractResourceWriter() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
  }

  @Override
  public T create(T resource, boolean dryRun) {
    return getResourceEndpoints(client)
        .inNamespace(resource.getMetadata().getNamespace())
        .resource(resource)
        .dryRun(dryRun)
        .create();
  }

  @Override
  public T update(T resource, boolean dryRun) {
    return getResourceEndpoints(client)
        .inNamespace(resource.getMetadata().getNamespace())
        .resource(resource)
        .dryRun(dryRun)
        .patch();
  }

  @Override
  public T update(T resource, String patch, boolean dryRun) {
    return getResourceEndpoints(client)
        .inNamespace(resource.getMetadata().getNamespace())
        .resource(resource)
        .dryRun(dryRun)
        .patch(patch);
  }

  @Override
  public T update(@Nonnull T resource, @Nonnull Consumer<T> setter) {
    return KubernetesClientUtil.retryOnConflict(
        () -> {
          T resourceToUpdate = getResourceEndpoints(client)
              .inNamespace(resource.getMetadata().getNamespace())
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
              .inNamespace(resource.getMetadata().getNamespace())
              .resource(resourceToUpdate)
              .lockResourceVersion(resourceToUpdate.getMetadata().getResourceVersion())
              .update();
        });
  }

  @Override
  public void delete(T resource, boolean dryRun) {
    getResourceEndpoints(client)
        .inNamespace(resource.getMetadata().getNamespace())
        .resource(resource)
        .dryRun(dryRun)
        .delete();
  }

  protected abstract MixedOperation<T, ?, ?> getResourceEndpoints(
      KubernetesClient client);

}
