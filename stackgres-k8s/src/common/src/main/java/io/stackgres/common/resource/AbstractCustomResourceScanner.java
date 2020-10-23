/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.stackgres.common.KubernetesClientFactory;

public abstract class AbstractCustomResourceScanner<T extends CustomResource,
    L extends CustomResourceList<T>, D extends CustomResourceDoneable<T>>
    implements CustomResourceScanner<T> {

  private final KubernetesClientFactory clientFactory;
  private final CustomResourceDefinitionContext customResourceDefinitionContext;
  private final Class<T> customResourceClass;
  private final Class<L> customResourceListClass;
  private final Class<D> customResourceDoneClass;

  protected AbstractCustomResourceScanner(KubernetesClientFactory clientFactory,
      CustomResourceDefinitionContext customResourceDefinitionContext,
      Class<T> customResourceClass,
      Class<L> customResourceListClass,
      Class<D> customResourceDoneClass) {
    super();
    this.clientFactory = clientFactory;
    this.customResourceDefinitionContext = customResourceDefinitionContext;
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
    this.customResourceDoneClass = customResourceDoneClass;
  }

  @Override
  public Optional<List<T>> findResources() {
    try (KubernetesClient client = clientFactory.create()) {
      return Optional.ofNullable(client.customResourceDefinitions()
          .withName(customResourceDefinitionContext.getName())
          .get())
          .map(crd -> client.customResources(customResourceDefinitionContext,
              customResourceClass,
              customResourceListClass,
              customResourceDoneClass)
          .inAnyNamespace()
          .list()
          .getItems());
    }
  }

  @Override
  public Optional<List<T>> findResources(String namespace) {
    try (KubernetesClient client = clientFactory.create()) {
      return Optional.ofNullable(client.customResourceDefinitions()
          .withName(customResourceDefinitionContext.getName())
          .get())
          .map(crd -> client.customResources(customResourceDefinitionContext,
              customResourceClass,
              customResourceListClass,
              customResourceDoneClass)
          .inNamespace(namespace)
          .list()
          .getItems());
    }
  }

  @Override
  public List<T> getResources() {
    try (KubernetesClient client = clientFactory.create()) {
      return client.customResources(customResourceDefinitionContext,
          customResourceClass,
          customResourceListClass,
          customResourceDoneClass)
          .list()
          .getItems();
    }
  }

  @Override
  public List<T> getResources(String namespace) {
    try (KubernetesClient client = clientFactory.create()) {
      return client.customResources(customResourceDefinitionContext,
          customResourceClass,
          customResourceListClass,
          customResourceDoneClass)
          .inNamespace(namespace)
          .list()
          .getItems();
    }
  }

}
