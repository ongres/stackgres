/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import org.jooq.lambda.Seq;

public interface ClusterContext extends EnvVarContext<StackGresCluster> {

  StackGresCluster getCluster();

  @Override
  default StackGresCluster getResource() {
    return getCluster();
  }

  @Override
  default Map<String, String> getEnvironmentVariables() {
    return Seq.of(ClusterEnvVar.values())
        .map(clusterStatefulSetEnvVars -> clusterStatefulSetEnvVars.envVar(getCluster()))
        .toMap(EnvVar::getName, EnvVar::getValue);
  }

}
