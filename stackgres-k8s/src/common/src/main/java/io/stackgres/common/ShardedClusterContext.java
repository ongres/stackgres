/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import org.jooq.lambda.Seq;

public interface ShardedClusterContext extends EnvVarContext<StackGresShardedCluster> {

  StackGresShardedCluster getShardedCluster();

  @Override
  default StackGresShardedCluster getResource() {
    return getShardedCluster();
  }

  @Override
  default Map<String, String> getEnvironmentVariables() {
    return Seq.of(ShardedClusterEnvVar.values())
        .map(clusterEnvVars -> clusterEnvVars.envVar(getShardedCluster()))
        .toMap(EnvVar::getName, EnvVar::getValue);
  }

}
