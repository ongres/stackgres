/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForUtil;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterCoordinatorClusterContextAppender {

  private final ShardedClusterCoordinatorPrimaryEndpointsContextAppender
      shardedClusterCoordinatorPrimaryEndpointsContextAppender;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final ObjectMapper objectMapper;

  public ShardedClusterCoordinatorClusterContextAppender(
      ShardedClusterCoordinatorPrimaryEndpointsContextAppender
          shardedClusterCoordinatorPrimaryEndpointsContextAppender,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      ObjectMapper objectMapper) {
    this.shardedClusterCoordinatorPrimaryEndpointsContextAppender =
        shardedClusterCoordinatorPrimaryEndpointsContextAppender;
    this.clusterFinder = clusterFinder;
    this.objectMapper = objectMapper;
  }

  public StackGresCluster appendContext(
      StackGresShardedCluster cluster,
      Builder contextBuilder,
      Optional<StackGresShardedCluster> replicateCluster) {
    StackGresCluster coordinator = getCoordinatorCluster(cluster, replicateCluster);
    contextBuilder.coordinator(coordinator);
    contextBuilder.foundCoordinatorCluster(clusterFinder.findByNameAndNamespace(
        coordinator.getMetadata().getName(), coordinator.getMetadata().getNamespace()));
    shardedClusterCoordinatorPrimaryEndpointsContextAppender.appendContext(coordinator, contextBuilder);
    return coordinator;
  }

  private StackGresCluster getCoordinatorCluster(
      StackGresShardedCluster original,
      Optional<StackGresShardedCluster> replicateCluster) {
    StackGresShardedCluster cluster = objectMapper.convertValue(original, StackGresShardedCluster.class);
    return StackGresShardedClusterForUtil.getCoordinatorCluster(cluster, replicateCluster);
  }

}
