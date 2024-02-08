/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.function.Function;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

public enum ClusterEnvVar implements EnvVarSource<StackGresCluster, ClusterContext> {
  POSTGRES_VERSION(context -> context.getSpec().getPostgres().getVersion()),
  POSTGRES_MAJOR_VERSION(context -> getPostgresFlavorComponent(context).get(context)
      .getMajorVersion(context.getSpec().getPostgres().getVersion())),
  POSTGRES_FLAVOR(context -> getPostgresFlavorComponent(context).get(context).getName()),
  BUILD_VERSION(context -> getPostgresFlavorComponent(context).get(context)
      .getBuildVersion(context.getSpec().getPostgres().getVersion())),
  BUILD_MAJOR_VERSION(context -> getPostgresFlavorComponent(context).get(context)
      .getBuildMajorVersion(context.getSpec().getPostgres().getVersion())),
  PATRONI_ENV("patroni"),
  BACKUP_ENV("backup"),
  RESTORE_ENV("restore"),
  REPLICATION_INITIALIZATION_ENV("replication-init"),
  REPLICATE_ENV("replicate"),
  POSTGRES_ENTRY_PORT(String.valueOf(EnvoyUtil.PG_ENTRY_PORT)),
  POSTGRES_REPL_ENTRY_PORT(String.valueOf(EnvoyUtil.PG_REPL_ENTRY_PORT)),
  POSTGRES_POOL_PORT(String.valueOf(EnvoyUtil.PG_POOL_PORT)),
  POSTGRES_PORT(String.valueOf(EnvoyUtil.PG_PORT));

  private final String substVar;
  private final Function<StackGresCluster, EnvVar> getEnvVar;

  ClusterEnvVar(String value) {
    this.substVar = getSubstVar();
    EnvVar envVar = new EnvVarBuilder()
        .withName(name())
        .withValue(value)
        .build();
    this.getEnvVar = context -> envVar;
  }

  ClusterEnvVar(Function<StackGresCluster, String> getValue) {
    this.substVar = getSubstVar();
    this.getEnvVar = context -> new EnvVarBuilder()
        .withName(name())
        .withValue(getValue.apply(context))
        .build();
  }

  @Override
  public String substVar() {
    return substVar;
  }

  @Override
  public Function<StackGresCluster, EnvVar> getEnvVar() {
    return getEnvVar;
  }

}
