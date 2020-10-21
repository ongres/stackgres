/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.ClusterStatefulSetEnvVars;

public enum PatroniEnvPaths {

  SHARED_MEMORY_PATH("/dev/shm"),
  ETC_PASSWD_PATH("/etc/passwd"),
  ETC_GROUP_PATH("/etc/group"),
  ETC_SHADOW_PATH("/etc/shadow"),
  ETC_GSHADOW_PATH("/etc/gshadow"),
  PATRONI_CONFIG_PATH("/etc/patroni"),
  LOCAL_BIN_PATH("/usr/local/bin"),
  LOCAL_BIN_START_PATRONI_SH_PATH(LOCAL_BIN_PATH, "start-patroni.sh"),
  LOCAL_BIN_POST_INIT_SH_PATH(LOCAL_BIN_PATH, "post-init.sh"),
  LOCAL_BIN_EXEC_WITH_ENV_PATH(LOCAL_BIN_PATH, "exec-with-env"),
  PG_BASE_PATH("/var/lib/postgresql"),
  PG_DATA_PATH(PG_BASE_PATH, "data"),
  PG_RUN_PATH("/var/run/postgresql"),
  PG_LOG_PATH("/var/log/postgresql"),
  BASE_ENV_PATH("/etc/env"),
  BASE_SECRET_PATH(BASE_ENV_PATH, ".secret"),
  TEMPLATES_PATH("/templates");

  private final String path;
  private final EnvVar envVar;

  PatroniEnvPaths(String path) {
    this.path = String.join("/", path);
    this.envVar = new EnvVarBuilder()
        .withName(name())
        .withValue(path)
        .build();
  }

  PatroniEnvPaths(PatroniEnvPaths parent, String path) {
    this(String.join("/", parent.path, path));
  }

  public static List<EnvVar> getEnvVars() {
    return Arrays.stream(PatroniEnvPaths.values()).map(PatroniEnvPaths::getEnvVar)
        .collect(Collectors.toUnmodifiableList());
  }

  public String getPath() {
    return path;
  }

  public EnvVar getEnvVar() {
    return envVar;
  }
}
