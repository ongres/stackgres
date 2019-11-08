/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;

import org.jooq.lambda.tuple.Tuple5;

public abstract class AbstractKubernetesCustomResourceFinder<T extends CustomResource>
    implements KubernetesCustomResourceFinder<T> {

  protected abstract Tuple5<KubernetesClientFactory, String, Class<T>,
      Class<? extends KubernetesResourceList<T>>, Class<? extends Doneable<T>>> arguments();

  /**
   * Will look for a customer resource by it's name in the given namespace.
   * @param name the name of the resource
   * @param namespace the namespace in which the resource should be located
   * @return the result of the search
   */
  public Optional<T> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = arguments().v1.create()) {
      return ResourceUtil.getCustomResource(client, arguments().v2)
          .map(cr -> client
              .customResources(cr,
                  arguments().v3,
                  arguments().v4,
                  arguments().v5)
              .inNamespace(namespace)
              .withName(name)
              .get());
    }
  }

}
