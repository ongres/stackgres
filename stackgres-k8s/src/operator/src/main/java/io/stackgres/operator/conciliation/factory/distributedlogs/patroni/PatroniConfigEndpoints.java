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

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.DefaultValues;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class PatroniConfigEndpoints
    implements ResourceGenerator<DistributedLogsContext> {

  public static final String PATRONI_CONFIG_KEY = "config";

  private final JsonMapper objectMapper;

  private final LabelFactory<StackGresDistributedLogs> labelFactory;

  @Inject
  public PatroniConfigEndpoints(
      JsonMapper objectMapper, LabelFactory<StackGresDistributedLogs> labelFactory) {
    this.objectMapper = objectMapper;
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(DistributedLogsContext context) {

    PatroniConfig patroniConf = new PatroniConfig();
    patroniConf.setTtl(30);
    patroniConf.setLoopWait(10);
    patroniConf.setRetryTimeout(10);
    patroniConf.setPostgresql(new PatroniConfig.PostgreSql());
    patroniConf.getPostgresql().setUsePgRewind(true);
    patroniConf.getPostgresql().setParameters(getPostgresConfigValues(context));

    final String patroniConfigJson = objectMapper.valueToTree(patroniConf).toString();

    StackGresDistributedLogs cluster = context.getSource();
    final Map<String, String> labels = labelFactory.patroniClusterLabels(cluster);

    return Stream.of(new EndpointsBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(configName(context))
        .withLabels(labels)
        .withAnnotations(ImmutableMap.of(PATRONI_CONFIG_KEY, patroniConfigJson))
        .withOwnerReferences(context.getOwnerReferences())
        .endMetadata()
        .build());
  }

  @NotNull
  public Map<String, String> getPostgresConfigValues(DistributedLogsContext context) {
    Map<String, String> params = new HashMap<>(DefaultValues.getDefaultValues());

    params.put("archive_command", "/bin/true");
    params.put("wal_level", "logical");
    params.put("wal_log_hints", "on");
    params.put("archive_mode", "on");
    params.put("shared_preload_libraries",
        "pg_stat_statements, auto_explain, timescaledb");
    params.put("timescaledb.telemetry_level", "off");

    return params;
  }

  private String configName(DistributedLogsContext context) {
    final String scope = labelFactory.clusterScope(context.getSource());
    return ResourceUtil.resourceName(scope + PatroniUtil.CONFIG_SERVICE);
  }

}
