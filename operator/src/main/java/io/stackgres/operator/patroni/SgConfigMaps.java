/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.QuarkusProfile;
import io.stackgres.common.ResourceUtils;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.customresources.sgcluster.StackGresCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgConfigMaps {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgConfigMaps.class);

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create the Service associated to the cluster.
   */
  public ConfigMap create(StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();
    final Integer pg_version = resource.getSpec().getPostgresVersion();

    Map<String, String> labels = ResourceUtils.defaultLabels(name);

    String patroniLabels = "{}";
    try {
      patroniLabels = new ObjectMapper().writeValueAsString(labels);
    } catch (JsonProcessingException e) {
      LOGGER.error("Error processing json: {}", e);
    }

    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_SCOPE", name);
    data.put("PATRONI_SUPERUSER_USERNAME", "postgres");
    data.put("PATRONI_KUBERNETES_USE_ENDPOINTS", "true");
    data.put("PATRONI_REPLICATION_USERNAME", "replicator");
    data.put("PATRONI_KUBERNETES_LABELS", patroniLabels);
    data.put("PATRONI_POSTGRESQL_LISTEN", "0.0.0.0:5432");
    data.put("PATRONI_RESTAPI_LISTEN", "0.0.0.0:8008");
    data.put("PATRONI_POSTGRESQL_DATA_DIR", "/var/lib/postgresql/data");
    data.put("PATRONI_POSTGRESQL_BIN_DIR", "/usr/lib/postgresql/" + pg_version + "/bin");
    data.put("PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY", "/run/postgresql");

    if (QuarkusProfile.getActiveProfile().isDev()) {
      data.put("PATRONI_LOG_LEVEL", "DEBUG");
    }

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      ConfigMap cm = new ConfigMapBuilder()
          .withNewMetadata()
          .withName(name)
          .withLabels(ResourceUtils.defaultLabels(name))
          .endMetadata()
          .withData(data)
          .build();

      client.configMaps().inNamespace(namespace).createOrReplace(cm);
      LOGGER.trace("ConfigMap: {}", cm);

      ConfigMapList list = client.configMaps().inNamespace(namespace).list();
      for (ConfigMap item : list.getItems()) {
        if (item.getMetadata().getName().equals(name)) {
          cm = item;
        }
      }
      ResourceUtils.logAsYaml(cm);

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
    final String namespace = resource.getMetadata().getNamespace();

    ConfigMap cm = client.configMaps().inNamespace(namespace).withName(name).get();
    if (cm != null) {
      client.configMaps().inNamespace(namespace).withName(name).delete();
      LOGGER.debug("Deleting ConfigMap: {}", name);
    }

    return cm;
  }

}
