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

import org.jooq.lambda.tuple.Tuple5;

public abstract class AbstractKubernetesCustomResourceScanner<T extends CustomResource,
    E extends KubernetesResourceList<T>> implements KubernetesResourceScanner<E> {

  protected abstract Tuple5<KubernetesClient, String, Class<T>,
      Class<E>, Class<? extends Doneable<T>>> arguments();

  @Override
  public Optional<E> findResources() {
    return ResourceUtil.getCustomResource(arguments().v1, arguments().v2)
        .map(cr -> arguments().v1.customResources(cr,
            arguments().v3,
            arguments().v4,
            arguments().v5)
            .inAnyNamespace()
            .list());
  }

  @Override
  public Optional<E> findResources(String namespace) {
    return ResourceUtil.getCustomResource(arguments().v1, arguments().v2)
        .map(cr -> arguments().v1.customResources(cr,
            arguments().v3,
            arguments().v4,
            arguments().v5)
            .inNamespace(namespace)
            .list());
  }

}
