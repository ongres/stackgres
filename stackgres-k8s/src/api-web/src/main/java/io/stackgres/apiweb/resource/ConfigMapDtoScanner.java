/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.apiweb.dto.configmap.ConfigMapDto;
import io.stackgres.apiweb.transformer.ConfigMapMapper;
import io.stackgres.common.resource.ResourceScanner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConfigMapDtoScanner implements ResourceScanner<ConfigMapDto> {

  private final KubernetesClient client;

  @Inject
  public ConfigMapDtoScanner(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public List<ConfigMapDto> findResources() {
    return client.configMaps().list().getItems().stream()
        .map(ConfigMapMapper::map)
        .toList();
  }

  @Override
  public List<ConfigMapDto> findResourcesInNamespace(String namespace) {
    return client.configMaps().inNamespace(namespace).list().getItems().stream()
        .map(ConfigMapMapper::map)
        .toList();
  }
}
