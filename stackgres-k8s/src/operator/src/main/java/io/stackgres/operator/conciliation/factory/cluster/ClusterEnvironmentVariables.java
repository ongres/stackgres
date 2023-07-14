/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterEnvironmentVariables
    implements SubResourceStreamFactory<EnvVar, StackGresClusterContext> {

  public Stream<EnvVar> streamResources(StackGresClusterContext context) {
    return Seq.of(ClusterStatefulSetPath.values())
        .map(clusterStatefulSetPath -> clusterStatefulSetPath.envVar(context))
        .append(Seq.of(ClusterStatefulSetEnvVars.values())
            .map(clusterStatefulEnvVar -> clusterStatefulEnvVar.envVar(context.getCluster())));
  }

}
