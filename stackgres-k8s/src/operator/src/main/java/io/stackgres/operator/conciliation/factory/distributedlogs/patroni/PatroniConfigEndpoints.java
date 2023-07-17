/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class PatroniConfigEndpoints
    implements ResourceGenerator<StackGresDistributedLogsContext> {

  public static final String PATRONI_CONFIG_KEY = "config";

  private final ObjectMapper objectMapper;

  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  @Inject
  public PatroniConfigEndpoints(
      ObjectMapper objectMapper, LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    this.objectMapper = objectMapper;
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    PatroniConfig patroniConf = new PatroniConfig();
    patroniConf.setTtl(30);
    patroniConf.setLoopWait(10);
    patroniConf.setRetryTimeout(10);
    patroniConf.setPostgresql(new PatroniConfig.PostgreSql());
    patroniConf.getPostgresql().setUsePgRewind(true);
    patroniConf.getPostgresql().setParameters(getPostgresConfigValues(context));

    final String patroniConfigJson = objectMapper.valueToTree(patroniConf).toString();

    StackGresDistributedLogs cluster = context.getSource();

    return Stream.of(new EndpointsBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(configName(context))
        .withLabels(labelFactory.clusterLabels(cluster))
        .withAnnotations(Map.of(PATRONI_CONFIG_KEY, patroniConfigJson))
        .endMetadata()
        .build());
  }

  @NotNull
  public Map<String, String> getPostgresConfigValues(StackGresDistributedLogsContext context) {
    final String version = StackGresDistributedLogsUtil.getPostgresVersion(context.getSource());
    Map<String, String> params = new HashMap<>(PostgresDefaultValues.getDefaultValues(
        StackGresVersion.getStackGresVersion(context.getSource()), version));
    Map<String, String> userParams = context.getPostgresConfig().getSpec().getPostgresqlConf();
    PostgresBlocklist.getBlocklistParameters().forEach(userParams::remove);
    params.putAll(userParams);

    params.put("archive_command", "/bin/true");
    params.put("dynamic_library_path", "$libdir:/opt/stackgres/lib");
    params.put("wal_level", "logical");
    params.put("wal_log_hints", "on");
    params.put("archive_mode", "on");
    params.put("shared_preload_libraries",
        "pg_stat_statements, auto_explain, timescaledb");
    params.put("timescaledb.telemetry_level", "off");

    return params;
  }

  private String configName(StackGresDistributedLogsContext context) {
    return PatroniUtil.configName(context.getSource());
  }

}
