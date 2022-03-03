/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.fabric8.kubernetes.client.CustomResource;

public abstract class AbstractDefaultCustomResourceHolder<T extends CustomResource<?, ?>>
    implements DefaultCustomResourceHolder<T> {

  private final Map<String, Set<String>> defaultResources = new HashMap<>();

  @Override
  public boolean isDefaultCustomResource(T customResource) {
    String name = customResource.getMetadata().getName();
    String namespace = customResource.getMetadata().getNamespace();
    return defaultResources.containsKey(namespace)
        && defaultResources.get(namespace).contains(name);
  }

  @Override
  public boolean isDefaultCustomResource(String name, String namespace) {
    return defaultResources.containsKey(namespace)
        && defaultResources.get(namespace).contains(name);
  }

  @Override
  public void holdDefaultCustomResource(T customResource) {
    String namespace = customResource.getMetadata().getNamespace();
    String name = customResource.getMetadata().getName();
    if (defaultResources.containsKey(namespace)) {
      defaultResources.get(namespace).add(name);
    } else {
      var namesSet = new HashSet<String>();
      namesSet.add(name);
      defaultResources.put(namespace, namesSet);
    }
  }
}
