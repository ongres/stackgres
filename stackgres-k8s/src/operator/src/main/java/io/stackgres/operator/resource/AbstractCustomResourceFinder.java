/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;

public abstract class AbstractCustomResourceFinder<T extends CustomResource>
    implements CustomResourceFinder<T> {

  private final KubernetesClientFactory clientFactory;
  private final String customResourceName;
  private final Class<T> customResourceClass;
  private final Class<? extends CustomResourceList<T>> customResourceListClass;
  private final Class<? extends CustomResourceDoneable<T>> customResourceDoneClass;

  protected AbstractCustomResourceFinder(KubernetesClientFactory clientFactory,
      String customResourceName, Class<T> customResourceClass,
      Class<? extends CustomResourceList<T>> customResourceListClass,
      Class<? extends CustomResourceDoneable<T>> customResourceDoneClass) {
    super();
    this.clientFactory = clientFactory;
    this.customResourceName = customResourceName;
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
    this.customResourceDoneClass = customResourceDoneClass;
  }

  /**
   * Will look for a customer resource by it's name in the given namespace.
   * @param name the name of the resource
   * @param namespace the namespace in which the resource should be located
   * @return the result of the search
   */
  public Optional<T> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = clientFactory.create()) {
      return ResourceUtil.getCustomResource(client, customResourceName)
          .map(crd -> Optional.ofNullable(client.customResources(crd,
              customResourceClass,
              customResourceListClass,
              customResourceDoneClass)
              .inNamespace(namespace)
              .withName(name)
              .get()))
          .orElseThrow(() -> new IllegalStateException("StackGres is not correctly installed:"
              + " CRD " + customResourceName + " not found."));
    }
  }

}
