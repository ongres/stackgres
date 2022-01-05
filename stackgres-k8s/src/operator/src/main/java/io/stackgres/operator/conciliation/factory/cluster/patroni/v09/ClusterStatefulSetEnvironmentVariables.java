/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.v09;

import java.util.List;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterEnvironmentVariablesFactory;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
public class ClusterStatefulSetEnvironmentVariables
    implements ClusterEnvironmentVariablesFactory<ClusterContext> {

  @Override
  public List<EnvVar> buildEnvironmentVariables(ClusterContext context) {
    return List.of(
        new EnvVarBuilder()
            .withName("ETC_PASSWD_PATH")
            .withValue("/etc/passwd")
            .build(),
        new EnvVarBuilder()
            .withName("ETC_GROUP_PATH")
            .withValue("/etc/group")
            .build(),
        new EnvVarBuilder()
            .withName("ETC_SHADOW_PATH")
            .withValue("/etc/shadow")
            .build(),
        new EnvVarBuilder()
            .withName("ETC_GSHADOW_PATH")
            .withValue("/etc/gshadow")
            .build(),
        new EnvVarBuilder()
            .withName("LOCAL_BIN_PATH")
            .withValue("/usr/local/bin")
            .build(),
        new EnvVarBuilder()
            .withName("LOCAL_BIN_SHELL_UTILS_PATH")
            .withValue("/usr/local/bin/shell-utils")
            .build(),
        new EnvVarBuilder()
            .withName("LOCAL_BIN_SETUP_DATA_PATHS_SH_PATH")
            .withValue("/usr/local/bin/setup-data-paths.sh")
            .build(),
        new EnvVarBuilder()
            .withName("LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH")
            .withValue("/usr/local/bin/setup-arbitrary-user.sh")
            .build(),
        new EnvVarBuilder()
            .withName("LOCAL_BIN_SETUP_SCRIPTS_SH_PATH")
            .withValue("/usr/local/bin/setup-scripts.sh")
            .build(),
        new EnvVarBuilder()
            .withName("LOCAL_BIN_START_PATRONI_SH_PATH")
            .withValue("/usr/local/bin/start-patroni.sh")
            .build(),
        new EnvVarBuilder()
            .withName("LOCAL_BIN_START_PATRONI_WITH_RESTORE_SH_PATH")
            .withValue("/usr/local/bin/start-patroni-with-restore.sh")
            .build(),
        new EnvVarBuilder()
            .withName("LOCAL_BIN_POST_INIT_SH_PATH")
            .withValue("/usr/local/bin/post-init.sh")
            .build(),
        new EnvVarBuilder()
            .withName("LOCAL_BIN_EXEC_WITH_ENV_PATH")
            .withValue("/usr/local/bin/exec-with-env")
            .build(),
        new EnvVarBuilder()
            .withName("LOCAL_BIN_CREATE_BACKUP_SH_PATH")
            .withValue("/usr/local/bin/create-backup.sh")
            .build(),
        new EnvVarBuilder()
            .withName("PG_BASE_PATH")
            .withValue("/var/lib/postgresql")
            .build(),
        new EnvVarBuilder()
            .withName("PG_DATA_PATH")
            .withValue("/var/lib/postgresql/data")
            .build(),
        new EnvVarBuilder()
            .withName("PG_RUN_PATH")
            .withValue("/var/run/postgresql")
            .build(),
        new EnvVarBuilder()
            .withName("PG_LOG_PATH")
            .withValue("/var/log/postgresql")
            .build(),
        new EnvVarBuilder()
            .withName("BASE_ENV_PATH")
            .withValue("/etc/env")
            .build(),
        new EnvVarBuilder()
            .withName("SHARED_MEMORY_PATH")
            .withValue("/dev/shm")
            .build(),
        new EnvVarBuilder()
            .withName("BASE_SECRET_PATH")
            .withValue("/etc/env/.secret")
            .build(),
        new EnvVarBuilder()
            .withName("PATRONI_ENV_PATH")
            .withValue("/etc/env/patroni")
            .build(),
        new EnvVarBuilder()
            .withName("PATRONI_CONFIG_PATH")
            .withValue("/etc/patroni")
            .build(),
        new EnvVarBuilder()
            .withName("BACKUP_ENV_PATH")
            .withValue("/etc/env/backup")
            .build(),
        new EnvVarBuilder()
            .withName("BACKUP_SECRET_PATH")
            .withValue("/etc/env/.secret/backup")
            .build(),
        new EnvVarBuilder()
            .withName("RESTORE_ENV_PATH")
            .withValue("/etc/env/restore")
            .build(),
        new EnvVarBuilder()
            .withName("RESTORE_SECRET_PATH")
            .withValue("/etc/env/.secret/restore")
            .build(),
        new EnvVarBuilder()
            .withName("TEMPLATES_PATH")
            .withValue("/templates")
            .build(),
        new EnvVarBuilder()
            .withName("PATRONI_ENV")
            .withValue("patroni")
            .build(),
        new EnvVarBuilder()
            .withName("BACKUP_ENV")
            .withValue("backup")
            .build(),
        new EnvVarBuilder()
            .withName("RESTORE_ENV")
            .withValue("restore")
            .build(),
        new EnvVarBuilder()
            .withName("POSTGRES_ENTRY_PORT")
            .withValue("7432")
            .build(),
        new EnvVarBuilder()
            .withName("POSTGRES_REPL_ENTRY_PORT")
            .withValue("7433")
            .build(),
        new EnvVarBuilder()
            .withName("POSTGRES_POOL_PORT")
            .withValue("6432")
            .build(),
        new EnvVarBuilder()
            .withName("POSTGRES_PORT")
            .withValue("5432")
            .build()
    );
  }
}
