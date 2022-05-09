/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.v11;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.AbstractPatroniConfigEndpoints;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V_1_1, stopAt = StackGresVersion.V_1_1)
public class PatroniConfigEndpoints extends AbstractPatroniConfigEndpoints {

  private static final List<String> EXTRA_BLOCKLISTED_PARAMETERS =
      List.of(new String[] {
          "listen_addresses",
          "huge_pages",
      });

  @Inject
  public PatroniConfigEndpoints(ObjectMapper objectMapper,
      LabelFactoryForCluster<StackGresCluster> labelFactory) {
    super(objectMapper, labelFactory);
  }

  public PatroniConfigEndpoints() {
    super(null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  protected Map<String, String> getPostgresConfigValues(StackGresClusterContext context) {
    return super.getPostgresConfigValues(context);
  }

  @Override
  protected Map<String, String> getPostgresRecoveryConfigValues(StackGresClusterContext context) {
    return super.getPostgresRecoveryConfigValues(context);
  }

  @Override
  protected PatroniConfig getPatroniConfig(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getCluster();
    PatroniConfig patroniConf = new PatroniConfig();
    patroniConf.setTtl(30);
    patroniConf.setLoopWait(10);
    patroniConf.setRetryTimeout(10);
    if (getPostgresFlavorComponent(cluster) != StackGresComponent.BABELFISH) {
      patroniConf.setCheckTimeline(true);
    }
    if (cluster.getSpec().getReplication().isSynchronousMode()) {
      patroniConf.setSynchronousMode(true);
      patroniConf.setSynchronousNodeCount(
          cluster.getSpec().getReplication().getSyncInstances());
    }
    if (cluster.getSpec().getReplication().isStrictSynchronousMode()) {
      patroniConf.setSynchronousModeStrict(true);
      patroniConf.setSynchronousNodeCount(
          cluster.getSpec().getReplication().getSyncInstances());
    }
    patroniConf.setPostgresql(new PatroniConfig.PostgreSql());
    patroniConf.getPostgresql().setUsePgRewind(true);
    patroniConf.getPostgresql().setParameters(getPostgresConfigValues(context));
    patroniConf.getPostgresql().setRecoveryConf(getPostgresRecoveryConfigValues(context));
    return patroniConf;
  }

  @Override
  protected Map<String, String> getPostgresParameters(StackGresClusterContext context,
      StackGresPostgresConfig pgConfig) {
    final String version = pgConfig.getSpec().getPostgresVersion();
    Map<String, String> params = new HashMap<>(
        PostgresDefaultValues.getDefaultValues(
            StackGresVersion.getStackGresVersion(context.getCluster()), version));
    Map<String, String> userParams = pgConfig.getSpec().getPostgresqlConf();
    PostgresBlocklist.getBlocklistParameters().forEach(userParams::remove);
    EXTRA_BLOCKLISTED_PARAMETERS.forEach(userParams::remove);
    params.putAll(userParams);

    params.put("port", String.valueOf(EnvoyUtil.PG_PORT));

    if (isBackupConfigurationPresent(context)) {
      params.put("archive_command",
          "exec-with-env '" + ClusterStatefulSetEnvVars.BACKUP_ENV.value(context
              .getSource()) + "'"
              + " -- wal-g wal-push %p");
    } else {
      params.put("archive_command", "/bin/true");
    }

    if (Optional.ofNullable(context.getSource())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getDistributedLogs)
        .map(StackGresClusterDistributedLogs::getDistributedLogs).isPresent()) {
      params.put("logging_collector", "on");
      params.put("log_destination", "csvlog");
      params.put("log_directory", ClusterStatefulSetPath.PG_LOG_PATH.path());
      params.put("log_filename", "postgres-%M.log");
      params.put("log_rotation_age", "30min");
      params.put("log_rotation_size", "0kB");
      params.put("log_truncate_on_rotation", "on");
    }

    if (getPostgresFlavorComponent(context.getSource()) == StackGresComponent.BABELFISH) {
      params.put("shared_preload_libraries", Optional.ofNullable(
          params.get("shared_preload_libraries"))
          .map(sharedPreloadLibraries -> "babelfishpg_tds, " + sharedPreloadLibraries)
          .orElse("babelfishpg_tds"));
    }

    return params;
  }

  @Override
  protected Map<String, String> getPostgresRecoveryParameters(StackGresClusterContext context,
      StackGresPostgresConfig pgConfig) {
    Map<String, String> params = new HashMap<>();

    if (isBackupConfigurationPresent(context)) {
      params.put("restore_command",
          "exec-with-env '" + ClusterStatefulSetEnvVars.BACKUP_ENV.value(context
              .getSource()) + "'"
              + " -- wal-g wal-fetch %f %p");
    }

    return params;
  }

}