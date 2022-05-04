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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.distributedlogs.StatefulSetDynamicVolumes;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
public class PatroniConfigMap implements VolumeFactory<StackGresDistributedLogsContext> {

  public static final int PATRONI_LOG_FILE_SIZE = 256 * 1024 * 1024;
  public static final String POSTGRES_PORT_NAME = "pgport";
  public static final String POSTGRES_REPLICATION_PORT_NAME = "pgreplication";

  private static final Logger PATRONI_LOGGER = LoggerFactory.getLogger("io.stackgres.patroni");

  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  private final ObjectMapper jsonMapper;

  @Inject
  public PatroniConfigMap(LabelFactoryForCluster<StackGresDistributedLogs> labelFactory,
      ObjectMapper jsonMapper) {
    this.labelFactory = labelFactory;
    this.jsonMapper = jsonMapper;
  }

  public static String name(StackGresDistributedLogsContext clusterContext) {
    final String name = clusterContext.getSource().getMetadata().getName();
    return StatefulSetDynamicVolumes.PATRONI_ENV.getResourceName(name);
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

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresDistributedLogsContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build()
    );
  }

  public @NotNull Volume buildVolume(StackGresDistributedLogsContext context) {
    return new VolumeBuilder()
        .withName(StatefulSetDynamicVolumes.PATRONI_ENV.getVolumeName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withDefaultMode(444)
            .withName(name(context))
            .build())
        .build();
  }

  public @NotNull HasMetadata buildSource(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();
    final String pgVersion = StackGresDistributedLogsUtil.getPostgresVersion(context.getSource());

    final String patroniClusterLabelsAsJson;
    final Map<String, String> patroniClusterLabels = labelFactory.genericLabels(cluster);
    try {
      patroniClusterLabelsAsJson = jsonMapper.writeValueAsString(
          patroniClusterLabels);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
    final String pgHost = "0.0.0.0"; // NOPMD
    final int pgRawPort = EnvoyUtil.PG_PORT;
    final int pgPort = EnvoyUtil.PG_PORT;
    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_SCOPE", clusterScope(cluster));
    data.put("PATRONI_KUBERNETES_SCOPE_LABEL",
        labelFactory.labelMapper().clusterScopeKey(cluster));
    data.put("PATRONI_KUBERNETES_LABELS", patroniClusterLabelsAsJson);
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

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(patroniClusterLabels)
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build();
  }

  public static String clusterScope(StackGresDistributedLogs distributedLogs) {
    return distributedLogs.getMetadata().getName();
  }

}
