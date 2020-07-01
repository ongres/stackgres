/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.operator.common.VolumePath;
import org.jooq.lambda.Seq;

public enum ClusterStatefulSetPath implements VolumePath {

  ETC_PASSWD_PATH("/etc/passwd"),
  ETC_GROUP_PATH("/etc/group"),
  ETC_SHADOW_PATH("/etc/shadow"),
  ETC_GSHADOW_PATH("/etc/gshadow"),
  LOCAL_BIN_PATH("/usr/local/bin"),
  PG_BASE_PATH("/var/lib/postgresql"),
  PG_DATA_PATH(PG_BASE_PATH, "data"),
  PG_RUN_PATH("/var/run/postgresql"),
  PG_LOG_PATH("/var/log/postgresql"),
  BASE_ENV_PATH("/etc/env"),
  BASE_SECRET_PATH(BASE_ENV_PATH, ".secret"),
  PATRONI_ENV_PATH(BASE_ENV_PATH, ClusterStatefulSetEnvVars.PATRONI_ENV.value()),
  PATRONI_CONFIG_PATH("/etc/patroni"),
  BACKUP_ENV_PATH(BASE_ENV_PATH, ClusterStatefulSetEnvVars.BACKUP_ENV.value()),
  BACKUP_SECRET_PATH(BASE_SECRET_PATH, ClusterStatefulSetEnvVars.BACKUP_ENV.value()),
  RESTORE_ENV_PATH(BASE_ENV_PATH, ClusterStatefulSetEnvVars.RESTORE_ENV.value()),
  RESTORE_SECRET_PATH(BASE_SECRET_PATH, ClusterStatefulSetEnvVars.RESTORE_ENV.value()),
  TEMPLATES_PATH("/templates");

  private final String path;
  private final String subPath;
  private final EnvVar envVar;

  ClusterStatefulSetPath(String path) {
    this.path = path;
    this.subPath = path.substring(1);
    this.envVar = new EnvVarBuilder()
        .withName(name())
        .withValue(path)
        .build();
  }

  ClusterStatefulSetPath(ClusterStatefulSetPath parent, String...paths) {
    this(Seq.of(parent.path).append(paths).toString("/"));
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public String subPath() {
    return subPath;
  }

  public EnvVar envVar() {
    return envVar;
  }
}
