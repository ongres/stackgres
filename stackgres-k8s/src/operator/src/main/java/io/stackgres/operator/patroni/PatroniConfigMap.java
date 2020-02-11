/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.cluster.ClusterStatefulSet;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.sidecars.envoy.Envoy;
import io.stackgres.operatorframework.resource.ResourceUtil;

import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniConfigMap implements StackGresClusterResourceStreamFactory {

  public static final String POSTGRES_PORT_NAME = "pgport";
  public static final String POSTGRES_REPLICATION_PORT_NAME = "pgreplication";

  private static final Logger PATRONI_LOGGER = LoggerFactory.getLogger("patroni");

  private final ObjectMapper objectMapper;

  @Inject
  public PatroniConfigMap(ObjectMapperProvider objectMapperProvider) {
    this.objectMapper = objectMapperProvider.objectMapper();
  }

  public static String name(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName());
  }

  @Override
  public Stream<HasMetadata> create(StackGresGeneratorContext context) {
    final String pgVersion = StackGresComponents.calculatePostgresVersion(
        context.getClusterContext().getCluster().getSpec().getPostgresVersion());

    final String patroniLabels;
    try {
      patroniLabels = objectMapper.writeValueAsString(
          StackGresUtil.patroniClusterLabels(context.getClusterContext().getCluster()));
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }

    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_SCOPE", StackGresUtil.clusterScope(context.getClusterContext().getCluster()));
    data.put("PATRONI_KUBERNETES_SCOPE_LABEL", StackGresUtil.clusterScopeKey());
    data.put("PATRONI_KUBERNETES_LABELS", patroniLabels);
    data.put("PATRONI_KUBERNETES_USE_ENDPOINTS", "true");
    data.put("PATRONI_SUPERUSER_USERNAME", "postgres");
    data.put("PATRONI_REPLICATION_USERNAME", "replicator");
    data.put("PATRONI_POSTGRESQL_LISTEN", "127.0.0.1:" + Envoy.PG_RAW_PORT);
    data.put("PATRONI_POSTGRESQL_CONNECT_ADDRESS",
        "${PATRONI_KUBERNETES_POD_IP}:" + Envoy.PG_RAW_ENTRY_PORT);

    data.put("PATRONI_RESTAPI_LISTEN", "0.0.0.0:8008");
    data.put("PATRONI_POSTGRESQL_DATA_DIR", ClusterStatefulSet.PG_DATA_PATH);
    data.put("PATRONI_POSTGRESQL_BIN_DIR", "/usr/lib/postgresql/" + pgVersion + "/bin");
    data.put("PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY", ClusterStatefulSet.PG_RUN_PATH);

    if (PATRONI_LOGGER.isTraceEnabled()) {
      data.put("PATRONI_LOG_LEVEL", "DEBUG");
    }

    return Seq.of(new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
        .withName(name(context.getClusterContext()))
        .withLabels(StackGresUtil.patroniClusterLabels(context.getClusterContext().getCluster()))
        .withOwnerReferences(ImmutableList.of(
            ResourceUtil.getOwnerReference(context.getClusterContext().getCluster())))
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build());
  }

}
