/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Load the coordinator SGCluster as it is deployed (with its status), since the coordinator
 * SGScript stores there values read from the coordinator that the SGShardedCluster uses to
 * generate the other SGClusters.
 */
@ApplicationScoped
public class ShardedClusterDeployedCoordinatorContextAppender
    extends ContextAppender<StackGresShardedCluster, Builder> {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  public ShardedClusterDeployedCoordinatorContextAppender(
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    contextBuilder.deployedCoordinator(clusterFinder.findByNameAndNamespace(
        StackGresShardedClusterUtil.getCoordinatorClusterName(cluster),
        cluster.getMetadata().getNamespace()));
  }

}
