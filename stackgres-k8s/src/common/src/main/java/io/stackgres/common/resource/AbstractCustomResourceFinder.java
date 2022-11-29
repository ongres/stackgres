/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;

public abstract class AbstractCustomResourceFinder<T extends CustomResource<?, ?>>
    implements CustomResourceFinder<T> {

  private final KubernetesClient client;
  private final Class<T> customResourceClass;
  private final Class<? extends DefaultKubernetesResourceList<T>> customResourceListClass;

  protected AbstractCustomResourceFinder(KubernetesClient client,
      Class<T> customResourceClass,
      Class<? extends DefaultKubernetesResourceList<T>> customResourceListClass) {
    this.client = client;
    this.customResourceClass = customResourceClass;
    this.customResourceListClass = customResourceListClass;
  }

  public AbstractCustomResourceFinder() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
    this.customResourceClass = null;
    this.customResourceListClass = null;
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
    return Optional.ofNullable(client.resources(
        customResourceClass, customResourceListClass)
        .inNamespace(namespace)
        .withName(name)
        .get());
  }

}
