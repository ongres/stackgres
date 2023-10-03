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

  LOCAL_BIN_PATH("/usr/local/bin"),
  LOCAL_BIN_SHELL_UTILS_PATH(LOCAL_BIN_PATH, "shell-utils"),
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
