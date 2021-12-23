/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import org.jooq.lambda.Seq;

public enum ClusterStatefulSetPath implements VolumePath {

  ETC_PASSWD_PATH("/etc/passwd"),
  ETC_GROUP_PATH("/etc/group"),
  ETC_SHADOW_PATH("/etc/shadow"),
  ETC_GSHADOW_PATH("/etc/gshadow"),
  SHARED_MEMORY_PATH("/dev/shm"),
  LOCAL_BIN_PATH("/usr/local/bin"),
  LOCAL_BIN_SHELL_UTILS_PATH(LOCAL_BIN_PATH, "shell-utils"),
  LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH(LOCAL_BIN_PATH, "setup-arbitrary-user.sh"),
  LOCAL_BIN_SETUP_DATA_PATHS_SH_PATH(LOCAL_BIN_PATH, "setup-data-paths.sh"),
  LOCAL_BIN_SETUP_SCRIPTS_SH_PATH(LOCAL_BIN_PATH, "setup-scripts.sh"),
  LOCAL_BIN_RELOCATE_BINARIES_SH_PATH(LOCAL_BIN_PATH, "relocate-binaries.sh"),
  LOCAL_BIN_START_PATRONI_SH_PATH(LOCAL_BIN_PATH, "start-patroni.sh"),
  LOCAL_BIN_START_PATRONI_WITH_RESTORE_SH_PATH(LOCAL_BIN_PATH, "start-patroni-with-restore.sh"),
  LOCAL_BIN_POST_INIT_SH_PATH(LOCAL_BIN_PATH, "post-init.sh"),
  LOCAL_BIN_EXEC_WITH_ENV_PATH(LOCAL_BIN_PATH, "exec-with-env"),
  LOCAL_BIN_CREATE_BACKUP_SH_PATH(LOCAL_BIN_PATH, "create-backup.sh"),
  LOCAL_BIN_SET_DBOPS_RUNNING_SH_PATH(LOCAL_BIN_PATH, "set-dbops-running.sh"),
  LOCAL_BIN_RUN_DBOPS_SH_PATH(LOCAL_BIN_PATH, "run-dbops.sh"),
  LOCAL_BIN_SET_DBOPS_RESULT_SH_PATH(LOCAL_BIN_PATH, "set-dbops-result.sh"),
  LOCAL_BIN_RUN_PGBENCH_SH_PATH(LOCAL_BIN_PATH, "dbops/pgbench/run-pgbench.sh"),
  LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH(LOCAL_BIN_PATH, "dbops/pgbench/set-pgbench-result.sh"),
  LOCAL_BIN_RUN_VACUUM_SH_PATH(LOCAL_BIN_PATH, "dbops/vacuum/run-vacuum.sh"),
  LOCAL_BIN_RUN_REPACK_SH_PATH(LOCAL_BIN_PATH, "dbops/repack/run-repack.sh"),
  LOCAL_BIN_RUN_MAJOR_VERSION_UPGRADE_SH_PATH(LOCAL_BIN_PATH,
      "dbops/major-version-upgrade/run-major-version-upgrade.sh"),
  LOCAL_BIN_RUN_RESTART_SH_PATH(LOCAL_BIN_PATH,
      "dbops/restart/run-restart.sh"),
  LOCAL_BIN_MAJOR_VERSION_UPGRADE_SH_PATH(LOCAL_BIN_PATH,
      "dbops/major-version-upgrade/major-version-upgrade.sh"),
  LOCAL_BIN_RESET_PATRONI_SH_PATH(LOCAL_BIN_PATH,
      "dbops/major-version-upgrade/reset-patroni.sh"),
  PG_BASE_PATH("/var/lib/postgresql"),
  PG_DATA_PATH(PG_BASE_PATH, "data"),
  PG_EXTENSIONS_BASE_PATH(PG_BASE_PATH, "extensions"),
  PG_EXTENSIONS_PATH(PG_EXTENSIONS_BASE_PATH,
      ClusterStatefulSetEnvVars.POSTGRES_MAJOR_VERSION.substVar(),
      ClusterStatefulSetEnvVars.BUILD_MAJOR_VERSION.substVar()),
  PG_EXTENSIONS_BINARIES_PATH(PG_EXTENSIONS_PATH,
      "usr/lib/postgresql",
      ClusterStatefulSetEnvVars.POSTGRES_MAJOR_VERSION.substVar()),
  PG_EXTENSIONS_BIN_PATH(PG_EXTENSIONS_BINARIES_PATH, "bin"),
  PG_EXTENSIONS_LIB_PATH(PG_EXTENSIONS_BINARIES_PATH, "lib"),
  PG_EXTENSIONS_SHARE_PATH(PG_EXTENSIONS_PATH,
      "usr/share/postgresql",
      ClusterStatefulSetEnvVars.POSTGRES_MAJOR_VERSION.substVar()),
  PG_EXTENSIONS_EXTENSION_PATH(PG_EXTENSIONS_SHARE_PATH, "extension"),
  PG_EXTENSIONS_LIB64_PATH(PG_EXTENSIONS_PATH, "usr/lib64"),
  PG_LIB64_PATH("/usr/lib64"),
  PG_BINARIES_PATH("/usr/lib/postgresql",
      ClusterStatefulSetEnvVars.POSTGRES_VERSION.substVar()),
  PG_BIN_PATH(PG_BINARIES_PATH, "bin"),
  PG_LIB_PATH(PG_BINARIES_PATH, "lib"),
  PG_EXTRA_BIN_PATH(PG_BINARIES_PATH, "extra/bin"),
  PG_EXTRA_LIB_PATH(PG_BINARIES_PATH, "extra/lib"),
  PG_SHARE_PATH("/usr/share/postgresql",
      ClusterStatefulSetEnvVars.POSTGRES_VERSION.substVar()),
  PG_EXTENSION_PATH(PG_SHARE_PATH, "extension"),
  PG_RELOCATED_PATH(PG_BASE_PATH, "relocated",
      ClusterStatefulSetEnvVars.POSTGRES_VERSION.substVar()),
  PG_RELOCATED_LIB64_PATH(PG_RELOCATED_PATH, "usr/lib64"),
  PG_RELOCATED_BINARIES_PATH(PG_RELOCATED_PATH, "usr/lib/postgresql",
      ClusterStatefulSetEnvVars.POSTGRES_VERSION.substVar()),
  PG_RELOCATED_BIN_PATH(PG_RELOCATED_BINARIES_PATH, "bin"),
  PG_RELOCATED_LIB_PATH(PG_RELOCATED_BINARIES_PATH, "lib"),
  PG_RELOCATED_SHARE_PATH(PG_RELOCATED_PATH, "usr/share/postgresql/",
      ClusterStatefulSetEnvVars.POSTGRES_VERSION.substVar()),
  PG_RELOCATED_EXTENSION_PATH(PG_RELOCATED_SHARE_PATH, "extension"),
  PG_UPGRADE_PATH(PG_BASE_PATH, "upgrade"),
  PG_RUN_PATH("/var/run/postgresql"),
  PG_LOG_PATH("/var/log/postgresql"),
  BASE_ENV_PATH("/etc/env"),
  BASE_SECRET_PATH(BASE_ENV_PATH, ".secret"),
  PATRONI_ENV_PATH(BASE_ENV_PATH, ClusterStatefulSetEnvVars.PATRONI_ENV.substVar()),
  PATRONI_CONFIG_PATH("/etc/patroni"),
  BACKUP_ENV_PATH(BASE_ENV_PATH, ClusterStatefulSetEnvVars.BACKUP_ENV.substVar()),
  BACKUP_SECRET_PATH(BASE_SECRET_PATH, ClusterStatefulSetEnvVars.BACKUP_ENV.substVar()),
  RESTORE_ENV_PATH(BASE_ENV_PATH, ClusterStatefulSetEnvVars.RESTORE_ENV.substVar()),
  RESTORE_SECRET_PATH(BASE_SECRET_PATH, ClusterStatefulSetEnvVars.RESTORE_ENV.substVar()),
  TEMPLATES_PATH("/templates"),
  SHARED_PATH("/shared"),
  PGBOUNCER_CONFIG_PATH("/etc/pgbouncer"),
  PGBOUNCER_CONFIG_FILE_PATH(PGBOUNCER_CONFIG_PATH, "pgbouncer.ini"),
  PGBOUNCER_AUTH_PATH(PGBOUNCER_CONFIG_PATH, "auth"),
  PGBOUNCER_AUTH_FILE_PATH(PGBOUNCER_AUTH_PATH, "users.txt");

  private final String path;

  ClusterStatefulSetPath(String path) {
    this.path = path;
  }

  ClusterStatefulSetPath(String... paths) {
    this(Seq.of(paths).toString("/"));
  }

  ClusterStatefulSetPath(ClusterStatefulSetPath parent, String... paths) {
    this(Seq.of(parent.path).append(paths).toString("/"));
  }

  @Override
  public String path() {
    return path(Map.of());
  }

  @Override
  public String path(ClusterContext context) {
    return path(context.getEnvironmentVariables());
  }

  @Override
  public String path(ClusterContext context, Map<String, String> envVars) {
    return path(envVars(context, envVars));
  }

  @Override
  public String path(Map<String, String> envVars) {
    StringBuilder path = new StringBuilder();
    int startIndexOf = this.path.indexOf("$(");
    int endIndexOf = -1;
    while (startIndexOf >= 0) {
      path.append(this.path, endIndexOf + 1, startIndexOf);
      endIndexOf = this.path.indexOf(")", startIndexOf);
      if (endIndexOf == -1) {
        throw new RuntimeException("Path " + this.path + " do not close variable substitution."
            + " Expected a `)` character after position " + startIndexOf);
      }
      String variable = this.path.substring(startIndexOf + 2, endIndexOf);
      String value = envVars.get(variable);
      if (value == null) {
        throw new RuntimeException("Path " + this.path + " specify variable " + variable
            + " for substitution. But was not found in map " + envVars);
      }
      path.append(value);
      startIndexOf = this.path.indexOf("$(", endIndexOf + 1);
    }
    if (endIndexOf == -1) {
      return this.path;
    }
    if (endIndexOf < this.path.length()) {
      path.append(this.path, endIndexOf + 1, this.path.length());
    }
    return path.toString();
  }

  @Override
  public String filename() {
    return filename(Map.of());
  }

  @Override
  public String filename(ClusterContext context) {
    return filename(context.getEnvironmentVariables());
  }

  @Override
  public String filename(ClusterContext context, Map<String, String> envVars) {
    return filename(envVars(context, envVars));
  }

  @Override
  public String filename(Map<String, String> envVars) {
    String path = path(envVars);
    int indexOfLastSlash = path.lastIndexOf('/');
    return indexOfLastSlash != -1 ? path.substring(indexOfLastSlash + 1) : path;
  }

  @Override
  public String subPath() {
    return subPath(Map.of());
  }

  @Override
  public String subPath(ClusterContext context) {
    return subPath(context.getEnvironmentVariables());
  }

  @Override
  public String subPath(ClusterContext context, Map<String, String> envVars) {
    return subPath(envVars(context, envVars));
  }

  @Override
  public String subPath(Map<String, String> envVars) {
    return path(envVars).substring(1);
  }

  @Override
  public String subPath(VolumePath relativeTo) {
    return relativize(subPath(Map.of()), relativeTo.subPath(Map.of()));
  }

  @Override
  public String subPath(ClusterContext context, VolumePath relativeTo) {
    return relativize(subPath(context.getEnvironmentVariables()),
        relativeTo.subPath(context.getEnvironmentVariables()));
  }

  @Override
  public String subPath(ClusterContext context, Map<String, String> envVars,
                        VolumePath relativeTo) {
    return relativize(subPath(envVars(context, envVars)),
        relativeTo.subPath(envVars(context, envVars)));
  }

  @Override
  public String subPath(Map<String, String> envVars, VolumePath relativeTo) {
    return relativize(subPath(envVars), relativeTo.subPath(envVars));
  }

  private String relativize(String subPath, String relativeToSubPath) {
    Preconditions.checkArgument(subPath.startsWith(relativeToSubPath + "/"),
        subPath + " is not relative to " + relativeToSubPath + "/");
    return subPath.substring(relativeToSubPath.length() + 1);
  }

  public EnvVar envVar() {
    return envVar(Map.of());
  }

  public EnvVar envVar(ClusterContext context) {
    return envVar(context.getEnvironmentVariables());
  }

  public EnvVar envVar(ClusterContext context, Map<String, String> envVars) {
    return envVar(envVars(context, envVars));
  }

  public EnvVar envVar(Map<String, String> envVars) {
    return new EnvVarBuilder()
        .withName(name())
        .withValue(path(envVars))
        .build();
  }

  private Map<String, String> envVars(ClusterContext context,
                                               Map<String, String> envVars) {
    Map<String, String> mergedEnvVars = Maps.newHashMap(context.getEnvironmentVariables());
    mergedEnvVars.putAll(envVars);
    return Map.copyOf(mergedEnvVars);
  }

}
