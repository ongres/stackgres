/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractCustomResourceScanner
    <T extends CustomResource<?, ?>, L extends CustomResourceList<T>>
    implements CustomResourceScanner<T> {

  private final KubernetesClientFactory clientFactory;

  private final Class<T> customResourceClass;
  private final Class<L> customResourceListClass;

  protected AbstractCustomResourceScanner(KubernetesClientFactory clientFactory,
      Class<T> customResourceClass,
      Class<L> customResourceListClass) {
    super();
    this.clientFactory = clientFactory;
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
  }

  @Override
  public Optional<List<T>> findResources() {
    String crdName = CustomResource.getCRDName(customResourceClass);
    try (KubernetesClient client = clientFactory.create()) {
      return Optional.ofNullable(client.apiextensions().v1().customResourceDefinitions()
          .withName(crdName)
          .get())
          .map(crd -> client.customResources(customResourceClass, customResourceListClass)
              .inAnyNamespace()
              .list()
              .getItems());
    }
  }

  @Override
  public Optional<List<T>> findResources(@Nullable String namespace) {
    String crdName = CustomResource.getCRDName(customResourceClass);
    try (KubernetesClient client = clientFactory.create()) {
      return Optional.ofNullable(client.apiextensions().v1().customResourceDefinitions()
          .withName(crdName)
          .get())
          .map(crd -> client.customResources(customResourceClass, customResourceListClass)
              .inNamespace(namespace)
              .list()
              .getItems());
    }
  }

  @Override
  public List<T> getResources() {
    try (KubernetesClient client = clientFactory.create()) {
      return client.customResources(customResourceClass, customResourceListClass)
          .inAnyNamespace()
          .list()
          .getItems();
    }
  }

  @Override
  public List<T> getResources(@Nullable String namespace) {
    try (KubernetesClient client = clientFactory.create()) {
      return client.customResources(customResourceClass, customResourceListClass)
          .inNamespace(namespace)
          .list()
          .getItems();
    }
  }

}
