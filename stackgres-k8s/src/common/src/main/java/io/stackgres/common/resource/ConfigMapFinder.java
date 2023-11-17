/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConfigMapFinder implements ResourceFinder<ConfigMap> {

  private final KubernetesClient client;

  @Inject
  public ConfigMapFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<ConfigMap> findByName(String name) {
    throw new UnsupportedOperationException("ConfigMaps are namespaced resources, try using "
        + "findByNameAndNamespace(String, String) to locale config maps");
  }

  @Override
  public Optional<ConfigMap> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(client.configMaps().inNamespace(namespace)
        .withName(name)
        .get());
  }

}
