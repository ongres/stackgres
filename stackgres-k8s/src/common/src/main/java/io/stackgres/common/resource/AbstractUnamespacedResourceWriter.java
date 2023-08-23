/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.CdiUtil;

public abstract class AbstractUnamespacedResourceWriter<T extends HasMetadata,
    L extends KubernetesResourceList<T>, R extends Resource<T>>
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
  public T create(T resource) {
    return getResourceEndpoints(client)
        .resource(resource)
        .create();
  }

  @Override
  public T update(T resource) {
    return getResourceEndpoints(client)
        .resource(resource)
        .patch();
  }

  @Override
  public T update(T resource, String patch) {
    return getResourceEndpoints(client)
        .resource(resource)
        .patch(patch);
  }

  @Override
  public void delete(T resource) {
    getResourceEndpoints(client)
        .resource(resource)
        .delete();
  }

  protected abstract NonNamespaceOperation<T, L, R> getResourceEndpoints(
      KubernetesClient client);

}
