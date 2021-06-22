/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni.v09;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;

public enum StatefulSetEnvPaths {

  ETC_PASSWD_PATH("/etc/passwd"),
  ETC_GROUP_PATH("/etc/group"),
  ETC_SHADOW_PATH("/etc/shadow"),
  ETC_GSHADOW_PATH("/etc/gshadow"),
  LOCAL_BIN_PATH("/usr/local/bin"),
  LOCAL_BIN_SHELL_UTILS_PATH(LOCAL_BIN_PATH, "shell-utils"),
  LOCAL_BIN_SETUP_DATA_PATHS_SH_PATH(LOCAL_BIN_PATH, "setup-data-paths.sh"),
  LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH(LOCAL_BIN_PATH, "setup-arbitrary-user.sh"),
  LOCAL_BIN_SETUP_SCRIPTS_SH_PATH(LOCAL_BIN_PATH, "setup-scripts.sh"),
  LOCAL_BIN_START_PATRONI_SH_PATH(LOCAL_BIN_PATH, "start-patroni.sh"),
  LOCAL_BIN_START_PATRONI_WITH_RESTORE_SH_PATH(LOCAL_BIN_PATH, "start-patroni-with-restore.sh"),
  LOCAL_BIN_POST_INIT_SH_PATH(LOCAL_BIN_PATH, "post-init.sh"),
  LOCAL_BIN_EXEC_WITH_ENV_PATH(LOCAL_BIN_PATH, "exec-with-env"),
  LOCAL_BIN_CREATE_BACKUP_SH_PATH(LOCAL_BIN_PATH, "create-backup.sh"),
  PG_BASE_PATH("/var/lib/postgresql"),
  PG_DATA_PATH(PG_BASE_PATH, "data"),
  PG_RUN_PATH("/var/run/postgresql"),
  PG_LOG_PATH("/var/log/postgresql"),
  BASE_ENV_PATH("/etc/env"),
  SHARED_MEMORY_PATH("/dev/shm"),
  BASE_SECRET_PATH(BASE_ENV_PATH, ".secret"),
  PATRONI_ENV_PATH(BASE_ENV_PATH, "patroni"),
  PATRONI_CONFIG_PATH("/etc/patroni"),
  BACKUP_ENV_PATH(BASE_ENV_PATH, "backup"),
  BACKUP_SECRET_PATH(BASE_SECRET_PATH, "backup"),
  RESTORE_ENV_PATH(BASE_ENV_PATH, "restore"),
  RESTORE_SECRET_PATH(BASE_SECRET_PATH, "restore"),
  TEMPLATES_PATH("/templates");

  private final String path;
  private final EnvVar envVar;

  StatefulSetEnvPaths(String path) {
    this.path = String.join("/", path);
    this.envVar = new EnvVarBuilder()
        .withName(name())
        .withValue(path)
        .build();
  }

  StatefulSetEnvPaths(StatefulSetEnvPaths parent, String path) {
    this(String.join("/", parent.path, path));
  }

  public static List<EnvVar> getEnvVars() {
    return Arrays.stream(StatefulSetEnvPaths.values()).map(StatefulSetEnvPaths::getEnvVar)
        .collect(Collectors.toUnmodifiableList());
  }

  public String getPath() {
    return path;
  }

  public EnvVar getEnvVar() {
    return envVar;
  }
}
