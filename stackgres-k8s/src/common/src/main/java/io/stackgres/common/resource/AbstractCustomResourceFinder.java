/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;

public abstract class AbstractCustomResourceFinder<T extends CustomResource<?, ?>>
    implements CustomResourceFinder<T> {

  private final KubernetesClientFactory clientFactory;
  private final Class<T> customResourceClass;
  private final Class<? extends CustomResourceList<T>> customResourceListClass;

  protected AbstractCustomResourceFinder(KubernetesClientFactory clientFactory,
      Class<T> customResourceClass,
      Class<? extends CustomResourceList<T>> customResourceListClass) {
    super();
    this.clientFactory = clientFactory;
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
  }

  /**
   * Will look for a customer resource by it's name in the given namespace.
   *
   * @param name the name of the resource
   * @param namespace the namespace in which the resource should be located
   * @return the result of the search
   */
  @Override
  public Optional<T> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = clientFactory.create()) {
      return Optional.ofNullable(client.customResources(
          customResourceClass, customResourceListClass)
          .inNamespace(namespace)
          .withName(name)
          .get());
    }
  }

}
