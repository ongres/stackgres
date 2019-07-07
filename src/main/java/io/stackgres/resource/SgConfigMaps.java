/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import io.stackgres.crd.sgcluster.StackGresCluster;
import io.stackgres.util.QuarkusProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("initialization.fields.uninitialized")
@ApplicationScoped
public class SgConfigMaps {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgConfigMaps.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create the Service associated to the cluster.
   */
  public ConfigMap create(StackGresCluster sgcluster) {
    final String name = sgcluster.getMetadata().getName();

    Map<String, String> labels = new HashMap<>();
    labels.put("app", "StackGres");
    labels.put("cluster-name", name);

    String patroniLabels = labels.entrySet().stream()
        .map(f -> f.getKey() + ": \"" + f.getValue() + "\"")
        .collect(Collectors.joining(", ", "{", "}"));

    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_SCOPE", name);
    data.put("PATRONI_KUBERNETES_NAMESPACE", namespace);
    data.put("PATRONI_SUPERUSER_USERNAME", "postgres");
    data.put("PATRONI_REPLICATION_USERNAME", "replication");
    data.put("PATRONI_KUBERNETES_USE_ENDPOINTS", "true");
    data.put("PATRONI_KUBERNETES_LABELS", patroniLabels);
    data.put("PATRONI_POSTGRESQL_LISTEN", "0.0.0.0");
    data.put("PATRONI_RESTAPI_LISTEN", "0.0.0.0:8008");
    data.put("PATRONI_POSTGRESQL_DATA_DIR", "/var/lib/postgresql/data");
    data.put("PATRONI_POSTGRESQL_BIN_DIR", "/usr/lib/postgresql/11/bin");
    data.put("PATRONI_CONFIG_DIR", "/var/lib/postgresql/data");
    data.put("PATRONI_POSTGRESQL_PORT", "5432");
    data.put("PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY", "/var/run/postgresql");

    if (QuarkusProfile.getActiveProfile().isDev()) {
      data.put("PATRONI_LOG_LEVEL", "DEBUG");
    }

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      ConfigMap cm = new ConfigMapBuilder()
          .withNewMetadata()
          .withName(name)
          .endMetadata()
          .withData(data)
          .build();

      client.configMaps().inNamespace(namespace).createOrReplace(cm);

      ConfigMapList list = client.configMaps().inNamespace(namespace).list();
      for (ConfigMap item : list.getItems()) {
        if (item.getMetadata().getName().equals(name)) {
          cm = item;
        }
      }

      LOGGER.debug("Creating ConfigMap: {}", name);
      return cm;
    }
  }

  /**
   * Delete resource.
   */
  public ConfigMap delete(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      return delete(client, resource);
    }
  }

  /**
   * Delete resource.
   */
  public ConfigMap delete(KubernetesClient client, StackGresCluster resource) {
    final String name = resource.getMetadata().getName();

    ConfigMap cm = client.configMaps().inNamespace(namespace).withName(name).get();
    if (cm != null) {
      client.configMaps().inNamespace(namespace).withName(name).delete();
      LOGGER.debug("Deleting ConfigMap: {}", name);
    }

    return cm;
  }

}
