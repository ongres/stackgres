/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Arrays;
import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

public enum ClusterPathV2 implements EnvVarPathSource<StackGresCluster> {

  ETC_PATH("/etc"),
  ETC_PASSWD_PATH(ETC_PATH, "passwd"),
  ETC_GROUP_PATH(ETC_PATH, "group"),
  ETC_SHADOW_PATH(ETC_PATH, "shadow"),
  ETC_GSHADOW_PATH(ETC_PATH, "gshadow"),
  ETC_POSTGRES_PATH(ETC_PATH, "postgresql"),
  SHARED_MEMORY_PATH("/dev/shm"),
  LOCAL_BIN_PATH("/usr/local/bin"),
  LOCAL_BIN_SHELL_UTILS_PATH(LOCAL_BIN_PATH, "shell-utils"),
  LOCAL_BIN_SETUP_FILESYSTEM_SH_PATH(LOCAL_BIN_PATH, "setup-filesystem.sh"),
  LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH(LOCAL_BIN_PATH, "setup-arbitrary-user.sh"),
  LOCAL_BIN_SETUP_SCRIPTS_SH_PATH(LOCAL_BIN_PATH, "setup-scripts.sh"),
  LOCAL_BIN_RELOCATE_BINARIES_SH_PATH(LOCAL_BIN_PATH, "relocate-binaries.sh"),
  LOCAL_BIN_START_PATRONI_SH_PATH(LOCAL_BIN_PATH, "start-patroni.sh"),
  LOCAL_BIN_START_PGBOUNCER_SH_PATH(LOCAL_BIN_PATH, "start-pgbouncer.sh"),
  LOCAL_BIN_PATRONICTL_PATH(LOCAL_BIN_PATH, "patronictl"),
  LOCAL_BIN_POST_INIT_SH_PATH(LOCAL_BIN_PATH, "post-init.sh"),
  LOCAL_BIN_EXEC_WITH_ENV_PATH(LOCAL_BIN_PATH, "exec-with-env"),
  LOCAL_BIN_COPY_EXTENSION_SG_PATH(LOCAL_BIN_PATH, "copy-extension.sh"),
  LOCAL_BIN_CREATE_BACKUP_SH_PATH(LOCAL_BIN_PATH, "create-backup.sh"),
  LOCAL_BIN_SET_DBOPS_RUNNING_SH_PATH(LOCAL_BIN_PATH, "set-dbops-running.sh"),
  LOCAL_BIN_RUN_DBOPS_SH_PATH(LOCAL_BIN_PATH, "run-dbops.sh"),
  LOCAL_BIN_SET_DBOPS_RESULT_SH_PATH(LOCAL_BIN_PATH, "set-dbops-result.sh"),
  LOCAL_BIN_RUN_PGBENCH_SH_PATH(LOCAL_BIN_PATH, "dbops/pgbench/run-pgbench.sh"),
  LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH(LOCAL_BIN_PATH, "dbops/pgbench/set-pgbench-result.sh"),
  LOCAL_BIN_RUN_SAMPLING_SH_PATH(LOCAL_BIN_PATH, "dbops/sampling/run-sampling.sh"),
  LOCAL_BIN_SET_SAMPLING_RESULT_SH_PATH(LOCAL_BIN_PATH, "dbops/sampling/set-sampling-result.sh"),
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
  LOCAL_BIN_START_FLUENTD_SH_PATH(LOCAL_BIN_PATH, "start-fluentd.sh"),
  LOCAL_BIN_START_POSTGRES_EXPORTER_SH_PATH(LOCAL_BIN_PATH, "start-postgres-exporter.sh"),
  HUGEPAGES_2M_PATH("/hugepages-2Mi"),
  HUGEPAGES_1G_PATH("/hugepages-1Gi"),
  PG_BASE_PATH("/var/db/postgresql"),
  PG_DATA_PATH(PG_BASE_PATH, "data"),
  PG_EXTENSIONS_BASE_PATH(PG_BASE_PATH, "extensions"),
  PG_REPLICATION_BASE_PATH(PG_BASE_PATH, "replication"),
  PG_REPLICATION_INITIALIZATION_FAILED_BACKUP_PATH(
      PG_REPLICATION_BASE_PATH, "initialization-failed-backup"),
  PG_EXTENSIONS_PATH(PG_EXTENSIONS_BASE_PATH,
      ClusterEnvVar.POSTGRES_VERSION.substVar(),
      ClusterEnvVar.BUILD_MAJOR_VERSION.substVar()),
  /**
   * The images of the StackGres registry install PostgreSQL under {@code /postgres/<version>}
   * (with {@code bin}, {@code lib}, {@code share} and {@code include}), the addons under
   * {@code /<addon>} (patroni, wal-g, hdrhistogram) and the system libraries under the Debian
   * multiarch directory {@code /usr/lib/x86_64-linux-gnu} ({@code /usr/lib64} only holds the
   * dynamic loader). The extensions layers and the relocated binaries mirror the same layout.
   */
  PG_EXTENSIONS_BINARIES_PATH(PG_EXTENSIONS_PATH,
      "postgres",
      ClusterEnvVar.POSTGRES_VERSION.substVar()),
  PG_EXTENSIONS_BIN_PATH(PG_EXTENSIONS_BINARIES_PATH, "bin"),
  PG_EXTENSIONS_LIB_PATH(PG_EXTENSIONS_BINARIES_PATH, "lib"),
  PG_EXTENSIONS_SHARE_PATH(PG_EXTENSIONS_BINARIES_PATH, "share"),
  PG_EXTENSIONS_EXTENSION_PATH(PG_EXTENSIONS_SHARE_PATH, "extension"),
  PG_EXTENSIONS_LIB64_PATH(PG_EXTENSIONS_PATH, "usr/lib64"),
  PG_EXTENSIONS_SYSTEM_LIB_PATH(PG_EXTENSIONS_PATH, "usr/lib/x86_64-linux-gnu"),
  USR_BIN_PATH("/usr/bin"),
  PG_LIB64_PATH("/usr/lib64"),
  PG_SYSTEM_LIB_PATH("/usr/lib/x86_64-linux-gnu"),
  PG_INSTALL_BASE_PATH("/postgres"),
  PG_BINARIES_PATH(PG_INSTALL_BASE_PATH,
      ClusterEnvVar.POSTGRES_VERSION.substVar()),
  PG_BIN_PATH(PG_BINARIES_PATH, "bin"),
  PG_LIB_PATH(PG_BINARIES_PATH, "lib"),
  PG_EXTRA_BIN_PATH(PG_BINARIES_PATH, "extra/bin"),
  PG_EXTRA_LIB_PATH(PG_BINARIES_PATH, "extra/lib"),
  PG_SHARE_PATH(PG_BINARIES_PATH, "share"),
  PG_EXTENSION_PATH(PG_SHARE_PATH, "extension"),
  PATRONI_PATH("/patroni"),
  PATRONI_BIN_PATH(PATRONI_PATH, "patroni"),
  PATRONICTL_BIN_PATH(PATRONI_PATH, "patronictl"),
  WALG_PATH("/wal-g"),
  WALG_BIN_PATH(WALG_PATH, "wal-g"),
  HDRHISTOGRAM_PATH("/hdrhistogram"),
  HDRHISTOGRAM_BIN_PATH(HDRHISTOGRAM_PATH, "dump_hdrh"),
  PGBOUNCER_PATH("/pgbouncer"),
  PGBOUNCER_BIN_PATH(PGBOUNCER_PATH, "pgbouncer"),
  POSTGRES_EXPORTER_PATH("/postgres-exporter"),
  POSTGRES_EXPORTER_BIN_PATH(POSTGRES_EXPORTER_PATH, "postgres_exporter"),
  FLUENTD_PATH("/fluentd"),
  FLUENTD_BIN_PATH(FLUENTD_PATH, "fluentd"),
  FLUENT_BIT_PATH("/fluent-bit"),
  FLUENT_BIT_BIN_PATH(FLUENT_BIT_PATH, "fluent-bit"),
  OTEL_COLLECTOR_PATH("/otel-collector"),
  OTEL_COLLECTOR_BIN_PATH(OTEL_COLLECTOR_PATH, "otelcol-contrib"),
  KUBECTL_PATH("/kubectl"),
  KUBECTL_BIN_PATH(KUBECTL_PATH, "kubectl"),
  PG_RELOCATED_BASE_PATH(PG_BASE_PATH, "relocated"),
  PG_RELOCATED_PATH(PG_RELOCATED_BASE_PATH,
      ClusterEnvVar.POSTGRES_VERSION.substVar(),
      ClusterEnvVar.BUILD_VERSION.substVar()),
  PG_RELOCATED_USR_BIN_PATH(PG_RELOCATED_PATH, "usr/bin"),
  PG_RELOCATED_LIB64_PATH(PG_RELOCATED_PATH, "usr/lib64"),
  PG_RELOCATED_SYSTEM_LIB_PATH(PG_RELOCATED_PATH, "usr/lib/x86_64-linux-gnu"),
  PG_RELOCATED_PG_PATH(PG_RELOCATED_PATH, "postgres",
      ClusterEnvVar.POSTGRES_VERSION.substVar()),
  PG_RELOCATED_BIN_PATH(PG_RELOCATED_PG_PATH, "bin"),
  PG_RELOCATED_LIB_PATH(PG_RELOCATED_PG_PATH, "lib"),
  PG_RELOCATED_SHARE_PATH(PG_RELOCATED_PG_PATH, "share"),
  PG_RELOCATED_EXTENSION_PATH(PG_RELOCATED_SHARE_PATH, "extension"),
  PG_UPGRADE_PATH(PG_BASE_PATH, "upgrade"),
  PG_RUN_PATH("/var/run/postgresql"),
  PG_LOG_PATH("/var/log/postgresql"),
  BASE_ENV_PATH(ETC_PATH, "env"),
  BASE_SECRET_PATH(BASE_ENV_PATH, ".secret"),
  PATRONI_ENV_PATH(BASE_ENV_PATH, ClusterEnvVar.PATRONI_ENV.substVar()),
  PATRONI_SECRET_ENV_PATH(BASE_SECRET_PATH, ClusterEnvVar.PATRONI_ENV.substVar()),
  PATRONI_CONFIG_PATH(ETC_PATH, "patroni"),
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
  PGBOUNCER_CONFIG_PATH(ETC_PATH, "pgbouncer"),
  PGBOUNCER_CONFIG_FILE_PATH(PGBOUNCER_CONFIG_PATH, "pgbouncer.ini"),
  PGBOUNCER_CONFIG_UPDATED_FILE_PATH(PGBOUNCER_CONFIG_PATH, "updated"),
  PGBOUNCER_AUTH_PATH(PGBOUNCER_CONFIG_PATH, "auth"),
  PGBOUNCER_AUTH_FILE_PATH(PGBOUNCER_AUTH_PATH, "users.txt"),
  PGBOUNCER_AUTH_TEMPLATE_FILE_PATH("/etc/pgbouncer-auth-users.template.txt"),
  FLUENT_BIT_LAST_CONFIG_PATH("/tmp", "last-fluentbit-conf");

  private final String path;

  ClusterPathV2(String path) {
    this.path = path;
  }

  ClusterPathV2(String... paths) {
    this(String.join("/", paths));
  }

  ClusterPathV2(ClusterPathV2 parent, String... paths) {
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
