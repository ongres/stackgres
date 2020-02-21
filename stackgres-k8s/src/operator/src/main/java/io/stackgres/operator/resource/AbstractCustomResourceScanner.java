/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;

public abstract class AbstractCustomResourceScanner<T extends CustomResource,
    L extends CustomResourceList<T>, D extends CustomResourceDoneable<T>>
    implements KubernetesCustomResourceScanner<T> {

  private final KubernetesClientFactory clientFactory;
  private final String customResourceName;
  private final Class<T> customResourceClass;
  private final Class<L> customResourceListClass;
  private final Class<D> customResourceDoneClass;

  protected AbstractCustomResourceScanner(KubernetesClientFactory clientFactory,
      String customResourceName, Class<T> customResourceClass, Class<L> customResourceListClass,
      Class<D> customResourceDoneClass) {
    super();
    this.clientFactory = clientFactory;
    this.customResourceName = customResourceName;
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
    this.customResourceDoneClass = customResourceDoneClass;
  }

  @Override
  public Optional<List<T>> findResources() {
    try (KubernetesClient client = clientFactory.create()) {
      return ResourceUtil.getCustomResource(client, customResourceName)
          .map(crd -> client.customResources(crd,
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
      return ResourceUtil.getCustomResource(client, customResourceName)
          .map(crd -> client.customResources(crd,
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
      return findResources()
          .orElseThrow(() -> new IllegalStateException("StackGres is not correctly installed:"
              + " CRD " + customResourceName + " not found."));
    }
  }

  @Override
  public List<T> getResources(String namespace) {
    try (KubernetesClient client = clientFactory.create()) {
      return findResources(namespace)
          .orElseThrow(() -> new IllegalStateException("StackGres is not correctly installed:"
              + " CRD " + customResourceName + " not found."));
    }
  }

}
