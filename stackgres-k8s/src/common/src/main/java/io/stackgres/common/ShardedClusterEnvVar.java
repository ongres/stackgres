/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.function.Function;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;

public enum ShardedClusterEnvVar
    implements EnvVarSource<StackGresShardedCluster, ShardedClusterContext> {
  POSTGRES_VERSION(context -> context.getSpec().getPostgres().getVersion()),
  POSTGRES_MAJOR_VERSION(context -> getPostgresFlavorComponent(context).get(context)
      .getMajorVersion(context.getSpec().getPostgres().getVersion())),
  POSTGRES_FLAVOR(context -> getPostgresFlavorComponent(context).get(context).getName()),
  BUILD_VERSION(context -> getPostgresFlavorComponent(context).get(context)
      .getBuildVersion(context.getSpec().getPostgres().getVersion())),
  BUILD_MAJOR_VERSION(context -> getPostgresFlavorComponent(context).get(context)
      .getBuildMajorVersion(context.getSpec().getPostgres().getVersion()));

  private final String substVar;
  private final Function<StackGresShardedCluster, EnvVar> getEnvVar;

  ShardedClusterEnvVar(String value) {
    this.substVar = getSubstVar();
    EnvVar envVar = new EnvVarBuilder()
        .withName(name())
        .withValue(value)
        .build();
    this.getEnvVar = context -> envVar;
  }

  ShardedClusterEnvVar(Function<StackGresShardedCluster, String> getValue) {
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
  public Function<StackGresShardedCluster, EnvVar> getEnvVar() {
    return getEnvVar;
  }

}
