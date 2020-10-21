/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class PatroniConfigMap implements ResourceGenerator<DistributedLogsContext> {

  public static final int PATRONI_LOG_FILE_SIZE = 256 * 1024 * 1024;
  public static final String POSTGRES_PORT_NAME = "pgport";
  public static final String POSTGRES_REPLICATION_PORT_NAME = "pgreplication";
  public static final String PATRONI_RESTAPI_PORT_NAME = "patroniport";

  private static final Logger PATRONI_LOGGER = LoggerFactory.getLogger("io.stackgres.patroni");

  private final LabelFactory<StackGresDistributedLogs> labelFactory;

  private final JsonMapper objectMapper;

  @Inject
  public PatroniConfigMap(LabelFactory<StackGresDistributedLogs> labelFactory,
                          JsonMapper objectMapper) {
    this.labelFactory = labelFactory;
    this.objectMapper = objectMapper;
  }

  public static String name(DistributedLogsContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getSource().getMetadata().getName());
  }

  @Override
  public Stream<HasMetadata> generateResource(DistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();
    final String pgVersion = StackGresComponent.POSTGRESQL.findVersion(
        StackGresComponent.LATEST);

    final String patroniLabels;
    final Map<String, String> value = labelFactory
        .patroniClusterLabels(cluster);
    try {
      patroniLabels = objectMapper.writeValueAsString(
          value);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
    final String pgHost = "0.0.0.0"; // NOPMD
    final int pgRawPort = EnvoyUtil.PG_PORT;
    final int pgPort = EnvoyUtil.PG_PORT;
    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_SCOPE", labelFactory.clusterScope(cluster));
    data.put("PATRONI_KUBERNETES_SCOPE_LABEL", labelFactory.getLabelMapper().clusterScopeKey());
    data.put("PATRONI_KUBERNETES_LABELS", patroniLabels);
    data.put("PATRONI_KUBERNETES_USE_ENDPOINTS", "true");
    data.put("PATRONI_KUBERNETES_PORTS", getKubernetesPorts(pgPort, pgRawPort));
    data.put("PATRONI_SUPERUSER_USERNAME", "postgres");
    data.put("PATRONI_REPLICATION_USERNAME", "replicator");
    data.put("PATRONI_POSTGRESQL_LISTEN", pgHost + ":" + EnvoyUtil.PG_PORT);
    data.put("PATRONI_POSTGRESQL_CONNECT_ADDRESS",
        "${PATRONI_KUBERNETES_POD_IP}:" + pgRawPort);

    data.put("PATRONI_RESTAPI_LISTEN", "0.0.0.0:8008");
    data.put("PATRONI_POSTGRESQL_DATA_DIR", PatroniEnvPaths.PG_DATA_PATH.getPath());
    data.put("PATRONI_POSTGRESQL_BIN_DIR", "/usr/lib/postgresql/" + pgVersion + "/bin");
    data.put("PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY", PatroniEnvPaths.PG_RUN_PATH.getPath());

    if (PATRONI_LOGGER.isTraceEnabled()) {
      data.put("PATRONI_LOG_LEVEL", "DEBUG");
    }

    data.put("PATRONI_SCRIPTS", "1");

    return Seq.of(new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(value)
        .withOwnerReferences(context.getOwnerReferences())
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build());
  }

  public static String getKubernetesPorts(final int pgPort, final int pgRawPort) {
    return "["
        + "{\"protocol\":\"TCP\","
        + "\"name\":\"" + POSTGRES_PORT_NAME + "\","
        + "\"port\":" + pgPort + "},"
        + "{\"protocol\":\"TCP\","
        + "\"name\":\"" + POSTGRES_REPLICATION_PORT_NAME + "\","
        + "\"port\":" + pgRawPort + "}"
        + "]";
  }

}
