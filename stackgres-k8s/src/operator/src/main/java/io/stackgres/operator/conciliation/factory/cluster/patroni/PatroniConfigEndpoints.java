/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.Blocklist;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.DefaultValues;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class PatroniConfigEndpoints
    implements ResourceGenerator<StackGresClusterContext> {

  public static final String PATRONI_CONFIG_KEY = "config";

  private final JsonMapper objectMapper;

  @Inject
  public PatroniConfigEndpoints(
      JsonMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {


    PatroniConfig patroniConf = new PatroniConfig();
    patroniConf.setTtl(30);
    patroniConf.setLoopWait(10);
    patroniConf.setRetryTimeout(10);
    patroniConf.setPostgresql(new PatroniConfig.PostgreSql());
    patroniConf.getPostgresql().setUsePgRewind(true);
    patroniConf.getPostgresql().setParameters(getPostgresConfigValues(context));

    final String patroniConfigJson = objectMapper.valueToTree(patroniConf).toString();

    final Map<String, String> labels = context.getPatroniClusterLabels();

    StackGresCluster cluster = context.getSource();
    return Stream.of(new EndpointsBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(configName(context))
        .withLabels(labels)
        .withAnnotations(ImmutableMap.of(PATRONI_CONFIG_KEY, patroniConfigJson))
        .endMetadata()
        .build());
  }

  @NotNull
  public Map<String, String> getPostgresConfigValues(StackGresClusterContext context) {
    Map<String, String> params = new HashMap<>(DefaultValues.getDefaultValues());

    if (isBackupConfigurationPresent(context)) {
      params.put("archive_command",
          "exec-with-env '" + ClusterStatefulSetEnvVars.BACKUP_ENV.value(context
          .getSource()) + "'"
              + " -- wal-g wal-push %p");
    } else {
      params.put("archive_command", "/bin/true");
    }

    params.put("dynamic_library_path",
        "$libdir:/opt/stackgres/lib");

    if (Optional.ofNullable(context.getSource())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getDistributedLogs)
        .map(StackGresClusterDistributedLogs::getDistributedLogs).isPresent()) {
      params.put("logging_collector", "on");
      params.put("log_destination", "csvlog");
      params.put("log_directory", ClusterStatefulSetPath.PG_LOG_PATH.path());
      params.put("log_filename", "postgres-%M.log");
      params.put("log_rotation_age", "30");
      params.put("log_rotation_size", "0");
      params.put("log_truncate_on_rotation", "on");
    }

    params.put("wal_level", "logical");
    params.put("wal_log_hints", "on");
    params.put("archive_mode", "on");

    StackGresPostgresConfig pgConfig = context.getPostgresConfig();

    Map<String, String> userParams = pgConfig.getSpec().getPostgresqlConf();
    for (String bl : Blocklist.getBlocklistParameters()) {
      userParams.remove(bl);
    }
    for (Map.Entry<String, String> userParam : userParams.entrySet()) {
      params.put(userParam.getKey(), userParam.getValue());
    }
    return params;
  }

  private String configName(StackGresClusterContext context) {
    final String scope = context.getClusterScope();
    return ResourceUtil.resourceName(scope + PatroniUtil.CONFIG_SERVICE);
  }

  private boolean isBackupConfigurationPresent(StackGresClusterContext context) {
    return context.getBackupConfig()
        .isPresent();
  }

}
