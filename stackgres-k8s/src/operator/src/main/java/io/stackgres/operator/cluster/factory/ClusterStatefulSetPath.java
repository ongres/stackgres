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

  LOCAL_BIN_PATH("/usr/local/bin"),
  PV_MOUNT_PATH("/var/lib"),
  PG_BASE_PATH(PV_MOUNT_PATH, "postgresql"),
  PG_RUN_PATH("/var/run/postgresql"),
  PG_DATA_PATH(PG_BASE_PATH, "data"),
  PG_LOG_PATH("/tmp"),
  BASE_ENV_PATH("/etc/env"),
  BASE_SECRET_PATH(BASE_ENV_PATH, ".secret"),
  PATRONI_ENV_PATH(BASE_ENV_PATH, ClusterStatefulSetEnvVars.PATRONI_ENV.value()),
  BACKUP_ENV_PATH(BASE_ENV_PATH, ClusterStatefulSetEnvVars.BACKUP_ENV.value()),
  BACKUP_SECRET_PATH(BASE_SECRET_PATH, ClusterStatefulSetEnvVars.BACKUP_ENV.value()),
  RESTORE_ENTRYPOINT_PATH("/etc/patroni/restore"),
  RESTORE_ENV_PATH(BASE_ENV_PATH, ClusterStatefulSetEnvVars.RESTORE_ENV.value()),
  RESTORE_SECRET_PATH(BASE_SECRET_PATH, ClusterStatefulSetEnvVars.RESTORE_ENV.value());

  private final String path;
  private final EnvVar envVar;

  ClusterStatefulSetPath(String path) {
    this.path = path;
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

  public EnvVar envVar() {
    return envVar;
  }
}
