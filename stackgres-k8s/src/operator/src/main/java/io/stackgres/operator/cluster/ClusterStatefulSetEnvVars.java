/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;

public enum ClusterStatefulSetEnvVars {
  PATRONI_ENV("patroni"),
  BACKUP_ENV("backup"),
  RESTORE_ENV("restore");

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
