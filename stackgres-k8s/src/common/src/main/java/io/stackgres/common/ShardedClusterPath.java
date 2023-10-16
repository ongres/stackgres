/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Arrays;
import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;

public enum ShardedClusterPath implements EnvVarPathSource<StackGresShardedCluster> {

  SHARED_PATH("/shared"),
  TEMPLATES_PATH("/templates"),
  LOCAL_BIN_PATH("/usr/local/bin"),
  LOCAL_BIN_SHELL_UTILS_PATH(LOCAL_BIN_PATH, "shell-utils"),
  LOCAL_BIN_SET_SHARDED_DBOPS_RUNNING_SH_PATH(LOCAL_BIN_PATH, "set-sharded-dbops-running.sh"),
  LOCAL_BIN_RUN_SHARDED_DBOPS_SH_PATH(LOCAL_BIN_PATH, "run-sharded-dbops.sh"),
  LOCAL_BIN_SET_SHARDED_DBOPS_RESULT_SH_PATH(LOCAL_BIN_PATH, "set-sharded-dbops-result.sh"),
  LOCAL_BIN_RUN_RESHARDING_SH_PATH(LOCAL_BIN_PATH, "dbops/resharding/run-resharding.sh"),
  LOCAL_BIN_RUN_SHARDED_MAJOR_VERSION_UPGRADE_SH_PATH(LOCAL_BIN_PATH,
      "sharded-dbops/major-version-upgrade/run-sharded-major-version-upgrade.sh"),
  LOCAL_BIN_RUN_SHARDED_MINOR_VERSION_UPGRADE_SH_PATH(LOCAL_BIN_PATH,
      "sharded-dbops/minor-version-upgrade/run-sharded-minor-version-upgrade.sh"),
  LOCAL_BIN_RUN_SHARDED_SECURITY_UPGRADE_SH_PATH(LOCAL_BIN_PATH,
      "sharded-dbops/security-upgrade/run-sharded-security-upgrade.sh"),
  LOCAL_BIN_RUN_SHARDED_RESTART_SH_PATH(LOCAL_BIN_PATH,
      "sharded-dbops/restart/run-sharded-restart.sh"),
  LOCAL_BIN_CREATE_SHARDED_BACKUP_SH_PATH(LOCAL_BIN_PATH, "create-sharded-backup.sh");

  private final String path;

  ShardedClusterPath(String path) {
    this.path = path;
  }

  ShardedClusterPath(String... paths) {
    this(String.join("/", paths));
  }

  ShardedClusterPath(ShardedClusterPath parent, String... paths) {
    this(parent.path, String.join("/", paths));
  }

  @Override
  public String rawPath() {
    return path;
  }

  public static List<EnvVar> envVars(ShardedClusterContext context) {
    return Arrays
        .stream(values())
        .map(path -> path.envVar(context))
        .toList();
  }

}
