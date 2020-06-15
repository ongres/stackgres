/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.operator.sidecars.envoy.Envoy;

public enum ClusterStatefulSetEnvVars {
  PATRONI_ENV("patroni"),
  BACKUP_ENV("backup"),
  RESTORE_ENV("restore"),
  POSTGRES_ENTRY_PORT(String.valueOf(Envoy.PG_ENTRY_PORT)),
  POSTGRES_REPL_ENTRY_PORT(String.valueOf(Envoy.PG_REPL_ENTRY_PORT)),
  POSTGRES_POOL_PORT(String.valueOf(Envoy.PG_POOL_PORT)),
  POSTGRES_PORT(String.valueOf(Envoy.PG_PORT));

  private final EnvVar envVar;

  ClusterStatefulSetEnvVars(String value) {
    this.envVar = new EnvVarBuilder()
        .withName(name())
        .withValue(value)
        .build();
  }

  public String value() {
    return envVar.getValue();
  }

  public EnvVar envVar() {
    return envVar;
  }
}
