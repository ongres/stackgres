/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.stackgres.operator.cluster.ClusterStatefulSet;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.resource.ResourceUtil;
import io.stackgres.operator.sidecars.envoy.Envoy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniConfigMap {

  public static final String POSTGRES_PORT_NAME = "pgport";
  public static final String POSTGRES_REPLICATION_PORT_NAME = "pgreplication";

  private static final Logger PATRONI_LOGGER = LoggerFactory.getLogger("patroni");

  private static final String PATRONI_SUFFIX = "-patroni";

  public static String patroniName(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + PATRONI_SUFFIX);
  }

  public ConfigMap createPatroniConfig(StackGresClusterContext context, ObjectMapper objectMapper) {
    final String pgVersion = StackGresComponents.calculatePostgresVersion(
        context.getCluster().getSpec().getPostgresVersion());

    final String patroniLabels;
    try {
      patroniLabels = objectMapper.writeValueAsString(
          ResourceUtil.patroniClusterLabels(context.getCluster()));
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }

    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_SCOPE", ResourceUtil.clusterScope(context.getCluster()));
    data.put("PATRONI_KUBERNETES_SCOPE_LABEL", ResourceUtil.clusterScopeKey());
    data.put("PATRONI_KUBERNETES_LABELS", patroniLabels);
    data.put("PATRONI_KUBERNETES_USE_ENDPOINTS", "true");
    data.put("PATRONI_SUPERUSER_USERNAME", "postgres");
    data.put("PATRONI_REPLICATION_USERNAME", "replicator");
    data.put("PATRONI_POSTGRESQL_LISTEN", "127.0.0.1:" + Envoy.PG_RAW_PORT);
    data.put("PATRONI_POSTGRESQL_CONNECT_ADDRESS",
        "${PATRONI_KUBERNETES_POD_IP}:" + Envoy.PG_RAW_ENTRY_PORT);

    data.put("PATRONI_RESTAPI_LISTEN", "0.0.0.0:8008");
    data.put("PATRONI_POSTGRESQL_DATA_DIR", ClusterStatefulSet.DATA_VOLUME_PATH);
    data.put("PATRONI_POSTGRESQL_BIN_DIR", "/usr/lib/postgresql/" + pgVersion + "/bin");
    data.put("PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY", "/run/postgresql");

    if (PATRONI_LOGGER.isTraceEnabled()) {
      data.put("PATRONI_LOG_LEVEL", "DEBUG");
    }

    data.put("PGDATA", ClusterStatefulSet.DATA_VOLUME_PATH);
    data.put("PGPORT", String.valueOf(Envoy.PG_RAW_PORT));
    data.put("PGUSER", "postgres");
    data.put("PGDATABASE", "postgres");
    data.put("PGHOST", "/run/postgresql");

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(context.getCluster().getMetadata().getNamespace())
        .withName(patroniName(context))
        .withLabels(ResourceUtil.patroniClusterLabels(context.getCluster()))
        .withOwnerReferences(ImmutableList.of(
            ResourceUtil.getOwnerReference(context.getCluster())))
        .endMetadata()
        .withData(data)
        .build();
  }

}
