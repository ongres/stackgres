/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.KeyToPathBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvVarPathSource;
import io.stackgres.common.ShardedClusterContext;
import io.stackgres.common.ShardedClusterPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import org.jooq.lambda.Unchecked;

public abstract class AbstractTemplatesVolumeFactory {

  public static final List<EnvVarPathSource<?>> SCRIPTS_PATHS = List.of(
      ClusterPath.LOCAL_BIN_SHELL_UTILS_PATH,
      ClusterPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH,
      ClusterPath.LOCAL_BIN_RELOCATE_BINARIES_SH_PATH,
      ClusterPath.LOCAL_BIN_START_PATRONI_SH_PATH,
      ClusterPath.LOCAL_BIN_START_PGBOUNCER_SH_PATH,
      ClusterPath.LOCAL_BIN_PATRONICTL_PATH,
      ClusterPath.LOCAL_BIN_POST_INIT_SH_PATH,
      ClusterPath.LOCAL_BIN_CREATE_BACKUP_SH_PATH,
      ClusterPath.LOCAL_BIN_EXEC_WITH_ENV_PATH,
      ClusterPath.LOCAL_BIN_SET_DBOPS_RUNNING_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_DBOPS_SH_PATH,
      ClusterPath.LOCAL_BIN_SET_DBOPS_RESULT_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_PGBENCH_SH_PATH,
      ClusterPath.LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_VACUUM_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_REPACK_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_MAJOR_VERSION_UPGRADE_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_RESTART_SH_PATH,
      ClusterPath.LOCAL_BIN_MAJOR_VERSION_UPGRADE_SH_PATH,
      ClusterPath.LOCAL_BIN_START_FLUENTBIT_SH_PATH,
      ClusterPath.LOCAL_BIN_START_POSTGRES_EXPORTER_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_CREATE_SHARDED_BACKUP_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_SET_SHARDED_DBOPS_RUNNING_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_DBOPS_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_SET_SHARDED_DBOPS_RESULT_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_RESHARDING_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_MAJOR_VERSION_UPGRADE_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_MINOR_VERSION_UPGRADE_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_SECURITY_UPGRADE_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_RESTART_SH_PATH);

  public static final List<ClusterPath> CLUSTER_TEMPLATE_PATHS = List.of(
      ClusterPath.LOCAL_BIN_SHELL_UTILS_PATH,
      ClusterPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH,
      ClusterPath.LOCAL_BIN_RELOCATE_BINARIES_SH_PATH,
      ClusterPath.LOCAL_BIN_START_PATRONI_SH_PATH,
      ClusterPath.LOCAL_BIN_START_PGBOUNCER_SH_PATH,
      ClusterPath.LOCAL_BIN_PATRONICTL_PATH,
      ClusterPath.LOCAL_BIN_POST_INIT_SH_PATH,
      ClusterPath.LOCAL_BIN_CREATE_BACKUP_SH_PATH,
      ClusterPath.LOCAL_BIN_EXEC_WITH_ENV_PATH,
      ClusterPath.ETC_PASSWD_PATH,
      ClusterPath.ETC_GROUP_PATH,
      ClusterPath.ETC_SHADOW_PATH,
      ClusterPath.ETC_GSHADOW_PATH,
      ClusterPath.LOCAL_BIN_SET_DBOPS_RUNNING_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_DBOPS_SH_PATH,
      ClusterPath.LOCAL_BIN_SET_DBOPS_RESULT_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_PGBENCH_SH_PATH,
      ClusterPath.LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_VACUUM_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_REPACK_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_MAJOR_VERSION_UPGRADE_SH_PATH,
      ClusterPath.LOCAL_BIN_RUN_RESTART_SH_PATH,
      ClusterPath.LOCAL_BIN_MAJOR_VERSION_UPGRADE_SH_PATH,
      ClusterPath.LOCAL_BIN_START_FLUENTBIT_SH_PATH,
      ClusterPath.LOCAL_BIN_START_POSTGRES_EXPORTER_SH_PATH);

  public static final List<ShardedClusterPath> SHARDED_CLUSTER_TEMPLATE_PATHS = List.of(
      ShardedClusterPath.LOCAL_BIN_SHELL_UTILS_PATH,
      ShardedClusterPath.LOCAL_BIN_CREATE_SHARDED_BACKUP_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_SET_SHARDED_DBOPS_RUNNING_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_DBOPS_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_SET_SHARDED_DBOPS_RESULT_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_RESHARDING_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_MAJOR_VERSION_UPGRADE_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_MINOR_VERSION_UPGRADE_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_SECURITY_UPGRADE_SH_PATH,
      ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_RESTART_SH_PATH);

  protected Map<String, String> getClusterTemplates() {
    Map<String, String> data = new HashMap<>();

    for (String resource : CLUSTER_TEMPLATE_PATHS
        .stream()
        .map(ClusterPath::filename)
        .toList()) {
      data.put(resource, Unchecked.supplier(() -> Resources
          .asCharSource(Objects.requireNonNull(AbstractTemplatesVolumeFactory.class
                  .getResource("/templates/" + resource)),
              StandardCharsets.UTF_8)
          .read()).get());
    }

    return data;
  }

  protected Volume buildVolumeForDistributedLogs(StackGresDistributedLogsContext context) {
    return buildVolumeForCluster(context.getSource().getMetadata().getName());
  }
  
  protected Volume buildVolumeForCluster(ClusterContext context) {
    return buildVolumeForCluster(context.getCluster().getMetadata().getName());
  }

  protected Volume buildVolumeForCluster(String clusterName) {
    return new VolumeBuilder()
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(StackGresVolume.SCRIPT_TEMPLATES.getResourceName(
                clusterName))
            .withItems(CLUSTER_TEMPLATE_PATHS.stream()
                .map(path -> new KeyToPathBuilder()
                    .withKey(path.filename())
                    .withPath(path.filename())
                    .withMode(SCRIPTS_PATHS.contains(path) ? 0555 : 0444)
                    .build())
                .toList())
            .withOptional(false)
            .build())
        .build();
  }

  protected Map<String, String> getShardedClusterTemplates() {
    Map<String, String> data = new HashMap<>();

    for (String resource : SHARDED_CLUSTER_TEMPLATE_PATHS
        .stream()
        .map(ShardedClusterPath::filename)
        .toList()) {
      data.put(resource, Unchecked.supplier(() -> Resources
          .asCharSource(Objects.requireNonNull(AbstractTemplatesVolumeFactory.class
                  .getResource("/templates/" + resource)),
              StandardCharsets.UTF_8)
          .read()).get());
    }

    return data;
  }

  protected Volume buildVolumeForShardedCluster(ShardedClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(StackGresVolume.SCRIPT_TEMPLATES.getResourceName(
                context.getShardedCluster().getMetadata().getName()))
            .withItems(SHARDED_CLUSTER_TEMPLATE_PATHS.stream()
                .map(path -> new KeyToPathBuilder()
                    .withKey(path.filename())
                    .withPath(path.filename())
                    .withMode(SCRIPTS_PATHS.contains(path) ? 0555 : 0444)
                    .build())
                .toList())
            .withOptional(false)
            .build())
        .build();
  }

}
