/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractCustomResourceScanner<T extends CustomResource<?, ?>,
    L extends DefaultKubernetesResourceList<T>>
    implements CustomResourceScanner<T> {

  private final KubernetesClient client;

  private final Class<T> customResourceClass;
  private final Class<L> customResourceListClass;

  protected AbstractCustomResourceScanner(KubernetesClient client,
      Class<T> customResourceClass,
      Class<L> customResourceListClass) {
    this.client = client;
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
  }

  public AbstractCustomResourceScanner() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
    this.customResourceClass = null;
    this.customResourceListClass = null;
  }

  @Override
  public Optional<List<T>> findResources() {
    String crdName = CustomResource.getCRDName(customResourceClass);
    return Optional.ofNullable(client.apiextensions().v1().customResourceDefinitions()
        .withName(crdName)
        .get())
        .map(crd -> client.resources(customResourceClass, customResourceListClass)
            .inAnyNamespace()
            .list()
            .getItems());
  }

  @Override
  public Optional<List<T>> findResources(@Nullable String namespace) {
    String crdName = CustomResource.getCRDName(customResourceClass);
    return Optional.ofNullable(client.apiextensions().v1().customResourceDefinitions()
        .withName(crdName)
        .get())
        .map(crd -> client.resources(customResourceClass, customResourceListClass)
            .inNamespace(namespace)
            .list()
            .getItems());
  }

  @Override
  public List<T> getResources() {
    return client.resources(customResourceClass, customResourceListClass)
        .inAnyNamespace()
        .list()
        .getItems();
  }

  @Override
  public List<T> getResources(@Nullable String namespace) {
    return client.resources(customResourceClass, customResourceListClass)
        .inNamespace(namespace)
        .list()
        .getItems();
  }

}
