/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.stackgres.common.QuarkusProfile;
import io.stackgres.common.StackGresClusterConfig;
import io.stackgres.common.resource.ResourceUtil;

public class PatroniConfigMap {

  /**
   * Create the ConfigMap associated with the cluster.
   */
  public static ConfigMap create(StackGresClusterConfig config, ObjectMapper objectMapper) {
    final String name = config.getCluster().getMetadata().getName();
    final String namespace = config.getCluster().getMetadata().getNamespace();
    final String pgVersion = config.getCluster().getSpec().getPostgresVersion();
    final String majorPgVersion = pgVersion.indexOf('.') > -1
        ? pgVersion.substring(0, pgVersion.indexOf('.')) : pgVersion;

    Map<String, String> labels = ResourceUtil.defaultLabels(name);

    final String patroniLabels;
    try {
      patroniLabels = objectMapper.writeValueAsString(labels);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
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
    data.put("PATRONI_POSTGRESQL_BIN_DIR", "/usr/lib/postgresql/" + majorPgVersion + "/bin");
    data.put("PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY", "/run/postgresql");

    if (QuarkusProfile.getActiveProfile().isDev()) {
      data.put("PATRONI_LOG_LEVEL", "DEBUG");
    }

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(ResourceUtil.defaultLabels(name))
        .endMetadata()
        .withData(data)
        .build();
  }

}
