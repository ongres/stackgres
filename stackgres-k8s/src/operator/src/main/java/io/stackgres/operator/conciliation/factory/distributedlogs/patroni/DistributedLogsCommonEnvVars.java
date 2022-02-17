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
import io.stackgres.common.EnvoyUtil;

public enum DistributedLogsCommonEnvVars {
  PATRONI_ENV("patroni"),
  BACKUP_ENV("backup"),
  RESTORE_ENV("restore"),
  POSTGRES_ENTRY_PORT(String.valueOf(EnvoyUtil.PG_ENTRY_PORT)),
  POSTGRES_REPL_ENTRY_PORT(String.valueOf(EnvoyUtil.PG_REPL_ENTRY_PORT)),
  POSTGRES_POOL_PORT(String.valueOf(EnvoyUtil.PG_POOL_PORT)),
  POSTGRES_PORT(String.valueOf(EnvoyUtil.PG_PORT));

  private final EnvVar envVar;

  DistributedLogsCommonEnvVars(String value) {
    this.envVar = new EnvVarBuilder()
        .withName(name())
        .withValue(value)
        .build();
  }

  public static List<EnvVar> getEnvVars() {
    return Arrays.stream(DistributedLogsCommonEnvVars.values())
        .map(DistributedLogsCommonEnvVars::envVar)
        .collect(Collectors.toUnmodifiableList());
  }

  public String value() {
    return envVar.getValue();
  }

  public EnvVar envVar() {
    return envVar;
  }
}
