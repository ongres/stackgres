/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;

@ApplicationScoped
public class ConfigMapFinder implements ResourceFinder<ConfigMap> {

  private final KubernetesClientFactory kubClientFactory;

  @Inject
  public ConfigMapFinder(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }

  @Override
  public Optional<ConfigMap> findByName(String name) {
    throw new UnsupportedOperationException("ConfigMaps are namespaced resources, try using "
        + "findByNameAndNamespace(String, String) to locale config maps");
  }

  @Override
  public Optional<ConfigMap> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return Optional.ofNullable(client.configMaps().inNamespace(namespace)
          .withName(name)
          .get());
    }
  }
}
