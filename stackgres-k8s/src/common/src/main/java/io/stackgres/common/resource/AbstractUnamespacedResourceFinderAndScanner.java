/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.CdiUtil;

public abstract class AbstractUnamespacedResourceFinderAndScanner<T extends HasMetadata>
    implements ResourceFinder<T>, ResourceScanner<T> {

  private final KubernetesClient client;

  public AbstractUnamespacedResourceFinderAndScanner(KubernetesClient client) {
    this.client = client;
  }

  public AbstractUnamespacedResourceFinderAndScanner() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
  }

  @Override
  public Optional<T> findByName(String name) {
    return Optional.ofNullable(getOperation(client)
        .withName(name)
        .get());
  }

  @Override
  public Optional<T> findByNameAndNamespace(String name, String namespace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<T> getResources() {
    return getOperation(client)
        .list()
        .getItems();
  }

  @Override
  public List<T> getResourcesWithLabels(Map<String, String> labels) {
    return getOperation(client)
        .withLabels(labels)
        .list()
        .getItems();
  }

  @Override
  public List<T> getResourcesInNamespace(String namespace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<T> getResourcesInNamespaceWithLabels(String namespace, Map<String, String> labels) {
    throw new UnsupportedOperationException();
  }

  protected abstract NonNamespaceOperation<T, ? extends KubernetesResourceList<T>, ? extends Resource<T>>
      getOperation(KubernetesClient client);

}
