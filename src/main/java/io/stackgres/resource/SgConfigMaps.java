/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.resource;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgConfigMaps {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgConfigMaps.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  @NonNull
  String namespace;

  @Inject
  @NonNull
  KubernetesClientFactory kubClientFactory;

  /**
   * Create the Service associated to the cluster.
   */
  public @NonNull ConfigMap create(@NonNull String configMapName, @NonNull String patroniScope) {
    LOGGER.debug("Creating service name: {}", configMapName);

    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_SCOPE", patroniScope);

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      ConfigMap cm = new ConfigMapBuilder()
          .withNewMetadata()
          .withName(configMapName)
          .endMetadata()
          .withData(data)
          .build();

      LOGGER.debug("Creating config map: {}", configMapName);

      client.configMaps().inNamespace(namespace).createOrReplace(cm);

      ConfigMapList list = client.configMaps().inNamespace(namespace).list();
      for (ConfigMap item : list.getItems()) {
        LOGGER.debug(item.getMetadata().getName());
        if (item.getMetadata().getName().equals(configMapName)) {
          cm = item;
        }
      }

      return cm;
    }
  }

}
