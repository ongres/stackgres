/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.sidecars.envoy.Envoy;
import io.stackgres.operator.sidecars.pooling.PgPooling;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniConfigMap implements StackGresClusterResourceStreamFactory {

  public static final int PATRONI_LOG_FILE_SIZE = 256 * 1024 * 1024;
  public static final String POSTGRES_PORT_NAME = "pgport";
  public static final String POSTGRES_REPLICATION_PORT_NAME = "pgreplication";
  public static final String PATRONI_RESTAPI_PORT_NAME = "patroniport";

  private static final Logger PATRONI_LOGGER = LoggerFactory.getLogger("io.stackgres.patroni");

  private ObjectMapper objectMapper = new ObjectMapper();

  private LabelFactoryDelegator factoryDelegator;

  public static String name(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName());
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getCluster();
    final String pgVersion = cluster.getSpec().getPostgresVersion();

    final String patroniLabels;
    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
    final Map<String, String> value = labelFactory
        .patroniClusterLabels(cluster);
    try {
      patroniLabels = objectMapper.writeValueAsString(
          value);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }

    final String pgHost = getPgHost(clusterContext);
    final int pgPort = getPgPort(clusterContext);
    final int pgRawPort = getPgRawPort(clusterContext);
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
    data.put("PATRONI_POSTGRESQL_DATA_DIR", ClusterStatefulSetPath.PG_DATA_PATH.path());
    data.put("PATRONI_POSTGRESQL_BIN_DIR", "/usr/lib/postgresql/" + pgVersion + "/bin");
    data.put("PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY", ClusterStatefulSetPath.PG_RUN_PATH.path());

    if (Optional.ofNullable(cluster.getSpec().getDistributedLogs())
        .map(StackGresClusterDistributedLogs::getDistributedLogs)
        .map(distributedLogs -> true)
        .orElse(false)) {
      data.put("PATRONI_LOG_DIR", ClusterStatefulSetPath.PG_LOG_PATH.path());
      data.put("PATRONI_LOG_FILE_NUM", "2");
      data.put("PATRONI_LOG_FILE_SIZE", String.valueOf(PATRONI_LOG_FILE_SIZE));
    }

    if (PATRONI_LOGGER.isTraceEnabled()) {
      data.put("PATRONI_LOG_LEVEL", "DEBUG");
    }

    data.put("PATRONI_SCRIPTS",
        Optional.ofNullable(
            cluster.getSpec().getInitData())
        .map(StackGresClusterInitData::getScripts)
        .map(List::size)
        .map(String::valueOf)
        .orElse("0"));

    return Seq.of(new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(clusterContext))
        .withLabels(value)
        .withOwnerReferences(clusterContext.getOwnerReferences())
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build());
  }

  public static int getPgRawPort(final StackGresClusterContext clusterContext) {
    return clusterContext.getSidecars().stream()
        .filter(entry -> entry.getSidecar() instanceof Envoy)
        .map(entry -> EnvoyUtil.PG_REPL_ENTRY_PORT)
        .findFirst()
        .orElse(EnvoyUtil.PG_PORT);
  }

  public static int getPgPort(final StackGresClusterContext clusterContext) {
    return Seq.of(
        clusterContext.getSidecars().stream()
        .filter(entry -> entry.getSidecar() instanceof Envoy)
        .map(entry -> EnvoyUtil.PG_ENTRY_PORT)
        .findFirst(),
        clusterContext.getSidecars().stream()
        .filter(entry -> entry.getSidecar() instanceof PgPooling)
        .map(entry -> EnvoyUtil.PG_POOL_PORT)
        .findFirst())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElse(EnvoyUtil.PG_PORT);
  }

  public static String getPgHost(final StackGresClusterContext clusterContext) {
    return clusterContext.getSidecars().stream()
        .filter(entry -> entry.getSidecar() instanceof Envoy)
        .map(entry -> "127.0.0.1") // NOPMD
        .findFirst()
        .orElse("0.0.0.0");  // NOPMD
  }

  public static String getKubernetesPorts(final StackGresClusterContext clusterContext) {
    return getKubernetesPorts(getPgPort(clusterContext), getPgRawPort(clusterContext));
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

  @Inject
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}
