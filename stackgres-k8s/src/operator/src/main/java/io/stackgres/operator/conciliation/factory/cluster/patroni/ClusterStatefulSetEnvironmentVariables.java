/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V11)
public class ClusterStatefulSetEnvironmentVariables
    implements ClusterEnvironmentVariablesFactory<ClusterContext> {

  @Override
  public List<EnvVar> buildEnvironmentVariables(ClusterContext context) {
    return Seq.of(ClusterStatefulSetPath.values())
        .map(clusterStatefulSetPath -> clusterStatefulSetPath.envVar(context))
        .append(Seq.of(ClusterStatefulSetEnvVars.values())
            .map(cssev -> cssev.envVar(context.getCluster())))
        .collect(Collectors.toUnmodifiableList());
  }
}
