/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.resource.ResourceScanner;

@ApplicationScoped
public class ConfigMapScanner implements ResourceScanner<ConfigMap> {

  private KubernetesClientFactory clientFactory;

  @Inject
  public ConfigMapScanner(KubernetesClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @Override
  public List<ConfigMap> findResources() {
    try (KubernetesClient client = clientFactory.create()) {
      return client.configMaps()
          .list().getItems();
    }
  }

  @Override
  public List<ConfigMap> findResourcesInNamespace(String namespace) {
    try (KubernetesClient client = clientFactory.create()) {
      return client.configMaps().inNamespace(namespace)
          .list().getItems();
    }
  }
}
