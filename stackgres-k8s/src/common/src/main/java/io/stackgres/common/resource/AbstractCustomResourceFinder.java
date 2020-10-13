/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.stackgres.common.KubernetesClientFactory;

public abstract class AbstractCustomResourceFinder<T extends CustomResource>
    implements CustomResourceFinder<T> {

  private final KubernetesClientFactory clientFactory;
  private final CustomResourceDefinitionContext customResourceDefinitionContext;
  private final Class<T> customResourceClass;
  private final Class<? extends CustomResourceList<T>> customResourceListClass;
  private final Class<? extends CustomResourceDoneable<T>> customResourceDoneClass;

  protected AbstractCustomResourceFinder(KubernetesClientFactory clientFactory,
      CustomResourceDefinitionContext customResourceDefinitionContext,
      Class<T> customResourceClass,
      Class<? extends CustomResourceList<T>> customResourceListClass,
      Class<? extends CustomResourceDoneable<T>> customResourceDoneClass) {
    super();
    this.clientFactory = clientFactory;
    this.customResourceDefinitionContext = customResourceDefinitionContext;
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
      return Optional.ofNullable(client.customResources(customResourceDefinitionContext,
          customResourceClass,
          customResourceListClass,
          customResourceDoneClass)
          .inNamespace(namespace)
          .withName(name)
          .get());
    }
  }

}
