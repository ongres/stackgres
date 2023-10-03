/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.ShardedClusterContext;
import io.stackgres.common.ShardedClusterEnvVar;
import io.stackgres.common.ShardedClusterPath;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class ShardedClusterEnvironmentVariables
    implements ShardedClusterEnvironmentVariablesFactory {

  @Override
  public List<EnvVar> buildEnvironmentVariables(ShardedClusterContext context) {
    return Seq.of(ShardedClusterPath.values())
        .map(clusterStatefulSetPath -> clusterStatefulSetPath.envVar(context))
        .append(Seq.of(ShardedClusterEnvVar.values())
            .map(cssev -> cssev.envVar(context.getShardedCluster())))
        .toList();
  }

}
