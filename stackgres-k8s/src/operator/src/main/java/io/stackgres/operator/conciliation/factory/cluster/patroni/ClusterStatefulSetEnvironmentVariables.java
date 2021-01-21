/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
public class ClusterStatefulSetEnvironmentVariables
    implements ClusterEnvironmentVariablesFactory<StackGresClusterContext> {

  @Override
  public List<EnvVar> buildEnvironmentVariables(StackGresClusterContext context) {
    return Seq.of(ClusterStatefulSetPath.values())
        .map(ClusterStatefulSetPath::envVar)
        .append(Seq.of(ClusterStatefulSetEnvVars.values())
            .map(cssev -> cssev.envVar(context.getSource())))
        .collect(Collectors.toUnmodifiableList());
  }
}
