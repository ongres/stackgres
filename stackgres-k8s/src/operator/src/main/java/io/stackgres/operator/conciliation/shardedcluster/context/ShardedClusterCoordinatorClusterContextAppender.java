/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForDdpUtil;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForShardingSphereUtil;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterCoordinatorClusterContextAppender {

  private final ShardedClusterCoordinatorPrimaryEndpointsContextAppender
      shardedClusterCoordinatorPrimaryEndpointsContextAppender;
  private final ObjectMapper objectMapper;

  public ShardedClusterCoordinatorClusterContextAppender(
      ShardedClusterCoordinatorPrimaryEndpointsContextAppender
          shardedClusterCoordinatorPrimaryEndpointsContextAppender,
      ObjectMapper objectMapper) {
    this.shardedClusterCoordinatorPrimaryEndpointsContextAppender =
        shardedClusterCoordinatorPrimaryEndpointsContextAppender;
    this.objectMapper = objectMapper;
  }

  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    StackGresCluster coordinator = getCoordinatorCluster(cluster);
    contextBuilder.coordinator(coordinator);
    shardedClusterCoordinatorPrimaryEndpointsContextAppender.appendContext(coordinator, contextBuilder);
  }

  private StackGresCluster getCoordinatorCluster(StackGresShardedCluster original) {
    StackGresShardedCluster cluster = objectMapper.convertValue(original, StackGresShardedCluster.class);
    switch (StackGresShardingType.fromString(cluster.getSpec().getType())) {
      case CITUS:
        return StackGresShardedClusterForCitusUtil.getCoordinatorCluster(cluster);
      case DDP:
        return StackGresShardedClusterForDdpUtil.getCoordinatorCluster(cluster);
      case SHARDING_SPHERE:
        return StackGresShardedClusterForShardingSphereUtil.getCoordinatorCluster(cluster);
      default:
        throw new UnsupportedOperationException(
            "Sharding technology " + cluster.getSpec().getType() + " not implemented");
    }
  }

}
