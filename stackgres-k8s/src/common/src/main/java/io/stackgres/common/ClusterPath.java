/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Arrays;
import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

public enum ClusterPath implements EnvVarPathSource<StackGresCluster> {

  ETC_PASSWD_PATH("/etc/passwd"),
  ETC_GROUP_PATH("/etc/group"),
  ETC_SHADOW_PATH("/etc/shadow"),
  ETC_GSHADOW_PATH("/etc/gshadow"),
  ETC_POSTGRES_PATH("/etc/postgresql"),
  SHARED_MEMORY_PATH("/dev/shm"),
  LOCAL_BIN_PATH("/usr/local/bin"),
  LOCAL_BIN_SHELL_UTILS_PATH(LOCAL_BIN_PATH, "shell-utils"),
  LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH(LOCAL_BIN_PATH, "setup-arbitrary-user.sh"),
  LOCAL_BIN_SETUP_SCRIPTS_SH_PATH(LOCAL_BIN_PATH, "setup-scripts.sh"),
  LOCAL_BIN_RELOCATE_BINARIES_SH_PATH(LOCAL_BIN_PATH, "relocate-binaries.sh"),
  LOCAL_BIN_START_PATRONI_SH_PATH(LOCAL_BIN_PATH, "start-patroni.sh"),
  LOCAL_BIN_START_PGBOUNCER_SH_PATH(LOCAL_BIN_PATH, "start-pgbouncer.sh"),
  LOCAL_BIN_PATRONICTL_PATH(LOCAL_BIN_PATH, "patronictl"),
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
  LOCAL_BIN_COPY_BINARIES_SH_PATH(LOCAL_BIN_PATH,
      "dbops/major-version-upgrade/copy-binaries.sh"),
  LOCAL_BIN_MAJOR_VERSION_UPGRADE_SH_PATH(LOCAL_BIN_PATH,
      "dbops/major-version-upgrade/major-version-upgrade.sh"),
  LOCAL_BIN_START_FLUENTBIT_SH_PATH(LOCAL_BIN_PATH, "start-fluentbit.sh"),
  LOCAL_BIN_START_POSTGRES_EXPORTER_SH_PATH(LOCAL_BIN_PATH, "start-postgres-exporter.sh"),
  HUGEPAGES_2M_PATH("/hugepages-2Mi"),
  HUGEPAGES_1G_PATH("/hugepages-1Gi"),
  PG_BASE_PATH("/var/lib/postgresql"),
  PG_DATA_PATH(PG_BASE_PATH, "data"),
  PG_EXTENSIONS_BASE_PATH(PG_BASE_PATH, "extensions"),
  PG_EXTENSIONS_PATH(PG_EXTENSIONS_BASE_PATH,
      ClusterEnvVar.POSTGRES_MAJOR_VERSION.substVar(),
      ClusterEnvVar.BUILD_MAJOR_VERSION.substVar()),
  PG_EXTENSIONS_BINARIES_PATH(PG_EXTENSIONS_PATH,
      "usr/lib/postgresql",
      ClusterEnvVar.POSTGRES_MAJOR_VERSION.substVar()),
  PG_EXTENSIONS_BIN_PATH(PG_EXTENSIONS_BINARIES_PATH, "bin"),
  PG_EXTENSIONS_LIB_PATH(PG_EXTENSIONS_BINARIES_PATH, "lib"),
  PG_EXTENSIONS_SHARE_PATH(PG_EXTENSIONS_PATH,
      "usr/share/postgresql",
      ClusterEnvVar.POSTGRES_MAJOR_VERSION.substVar()),
  PG_EXTENSIONS_EXTENSION_PATH(PG_EXTENSIONS_SHARE_PATH, "extension"),
  PG_EXTENSIONS_LIB64_PATH(PG_EXTENSIONS_PATH, "usr/lib64"),
  PG_LIB64_PATH("/usr/lib64"),
  PG_BINARIES_PATH("/usr/lib/postgresql",
      ClusterEnvVar.POSTGRES_VERSION.substVar()),
  PG_BIN_PATH(PG_BINARIES_PATH, "bin"),
  PG_LIB_PATH(PG_BINARIES_PATH, "lib"),
  PG_EXTRA_BIN_PATH(PG_BINARIES_PATH, "extra/bin"),
  PG_EXTRA_LIB_PATH(PG_BINARIES_PATH, "extra/lib"),
  PG_SHARE_PATH("/usr/share/postgresql",
      ClusterEnvVar.POSTGRES_VERSION.substVar()),
  PG_EXTENSION_PATH(PG_SHARE_PATH, "extension"),
  PG_RELOCATED_BASE_PATH(PG_BASE_PATH, "relocated"),
  PG_RELOCATED_PATH(PG_RELOCATED_BASE_PATH,
      ClusterEnvVar.POSTGRES_VERSION.substVar(),
      ClusterEnvVar.BUILD_VERSION.substVar()),
  PG_RELOCATED_LIB64_PATH(PG_RELOCATED_PATH, "usr/lib64"),
  PG_RELOCATED_BINARIES_PATH(PG_RELOCATED_PATH, "usr/lib/postgresql",
      ClusterEnvVar.POSTGRES_VERSION.substVar()),
  PG_RELOCATED_BIN_PATH(PG_RELOCATED_BINARIES_PATH, "bin"),
  PG_RELOCATED_LIB_PATH(PG_RELOCATED_BINARIES_PATH, "lib"),
  PG_RELOCATED_SHARE_PATH(PG_RELOCATED_PATH, "usr/share/postgresql",
      ClusterEnvVar.POSTGRES_VERSION.substVar()),
  PG_RELOCATED_EXTENSION_PATH(PG_RELOCATED_SHARE_PATH, "extension"),
  PG_UPGRADE_PATH(PG_BASE_PATH, "upgrade"),
  PG_RUN_PATH("/var/run/postgresql"),
  PG_LOG_PATH("/var/log/postgresql"),
  BASE_ENV_PATH("/etc/env"),
  BASE_SECRET_PATH(BASE_ENV_PATH, ".secret"),
  PATRONI_ENV_PATH(BASE_ENV_PATH, ClusterEnvVar.PATRONI_ENV.substVar()),
  PATRONI_SECRET_ENV_PATH(BASE_SECRET_PATH, ClusterEnvVar.PATRONI_ENV.substVar()),
  PATRONI_CONFIG_PATH("/etc/patroni"),
  PATRONI_CONFIG_FILE_PATH(PATRONI_CONFIG_PATH, "config.yml"),
  BACKUP_ENV_PATH(BASE_ENV_PATH, ClusterEnvVar.BACKUP_ENV.substVar()),
  BACKUP_SECRET_PATH(BASE_SECRET_PATH, ClusterEnvVar.BACKUP_ENV.substVar()),
  RESTORE_ENV_PATH(BASE_ENV_PATH, ClusterEnvVar.RESTORE_ENV.substVar()),
  RESTORE_SECRET_PATH(BASE_SECRET_PATH, ClusterEnvVar.RESTORE_ENV.substVar()),
  REPLICATION_INITIALIZATION_ENV_PATH(BASE_ENV_PATH, ClusterEnvVar.REPLICATION_INITIALIZATION_ENV.substVar()),
  REPLICATION_INITIALIZATION_SECRET_PATH(BASE_SECRET_PATH, ClusterEnvVar.REPLICATION_INITIALIZATION_ENV.substVar()),
  REPLICATE_ENV_PATH(BASE_ENV_PATH, ClusterEnvVar.REPLICATE_ENV.substVar()),
  REPLICATE_SECRET_PATH(BASE_SECRET_PATH, ClusterEnvVar.REPLICATE_ENV.substVar()),
  SSL_PATH("/etc/ssl"),
  SSL_COPY_PATH("/etc/ssl-copy"),
  TEMPLATES_PATH("/templates"),
  SHARED_PATH("/shared"),
  PGBOUNCER_CONFIG_PATH("/etc/pgbouncer"),
  PGBOUNCER_CONFIG_FILE_PATH(PGBOUNCER_CONFIG_PATH, "pgbouncer.ini"),
  PGBOUNCER_CONFIG_UPDATED_FILE_PATH(PGBOUNCER_CONFIG_PATH, "updated"),
  PGBOUNCER_AUTH_PATH(PGBOUNCER_CONFIG_PATH, "auth"),
  PGBOUNCER_AUTH_FILE_PATH(PGBOUNCER_AUTH_PATH, "users.txt"),
  FLUENT_BIT_LAST_CONFIG_PATH("/tmp", "last-fluentbit-conf");

  private final String path;

  ClusterPath(String path) {
    this.path = path;
  }

  ClusterPath(String... paths) {
    this(String.join("/", paths));
  }

  ClusterPath(ClusterPath parent, String... paths) {
    this(parent.path, String.join("/", paths));
  }

  @Override
  public String rawPath() {
    return path;
  }

  public static List<EnvVar> envVars(ClusterContext context) {
    return Arrays
        .stream(values())
        .map(path -> path.envVar(context))
        .toList();
  }

}
