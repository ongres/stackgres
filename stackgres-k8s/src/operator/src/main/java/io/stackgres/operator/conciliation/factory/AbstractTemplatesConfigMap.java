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
import io.stackgres.common.ClusterPath;
import io.stackgres.common.ShardedClusterPath;
import org.jooq.lambda.Unchecked;

public abstract class AbstractTemplatesConfigMap<T>
    implements VolumeFactory<T> {

  public static final List<ClusterPath> CLUSTER_TEMPLATE_PATHS = List.of(
      ClusterPath.LOCAL_BIN_SHELL_UTILS_PATH,
      ClusterPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH,
      ClusterPath.LOCAL_BIN_SETUP_SCRIPTS_SH_PATH,
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
      ClusterPath.LOCAL_BIN_RESET_PATRONI_SH_PATH,
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
          .asCharSource(Objects.requireNonNull(AbstractTemplatesConfigMap.class
                  .getResource("/templates/" + resource)),
              StandardCharsets.UTF_8)
          .read()).get());
    }

    return data;
  }

  protected Map<String, String> getShardedClusterTemplates() {
    Map<String, String> data = new HashMap<>();

    for (String resource : SHARDED_CLUSTER_TEMPLATE_PATHS
        .stream()
        .map(ShardedClusterPath::filename)
        .toList()) {
      data.put(resource, Unchecked.supplier(() -> Resources
          .asCharSource(Objects.requireNonNull(AbstractTemplatesConfigMap.class
                  .getResource("/templates/" + resource)),
              StandardCharsets.UTF_8)
          .read()).get());
    }

    return data;
  }

}
