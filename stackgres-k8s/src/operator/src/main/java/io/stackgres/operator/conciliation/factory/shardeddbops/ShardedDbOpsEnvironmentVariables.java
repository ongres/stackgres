/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.ShardedClusterEnvVar;
import io.stackgres.common.ShardedClusterPath;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ShardedDbOpsEnvironmentVariables
    implements SubResourceStreamFactory<EnvVar, StackGresShardedDbOpsContext> {

  public Stream<EnvVar> streamResources(StackGresShardedDbOpsContext context) {
    return Seq.of(ShardedClusterPath.values())
        .map(clusterStatefulSetPath -> clusterStatefulSetPath.envVar(context))
        .append(Seq.of(ShardedClusterEnvVar.values())
            .map(clusterStatefulEnvVar -> clusterStatefulEnvVar.envVar(
                context.getShardedCluster())));
  }

}
