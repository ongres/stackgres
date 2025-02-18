/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops.context;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;

import java.util.Optional;

import io.stackgres.common.ShardedDbOpsUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedDbOpsClusterCoordinatorContextAppender
    extends ContextAppender<StackGresShardedDbOps, Builder> {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  public ShardedDbOpsClusterCoordinatorContextAppender(CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  public void appendContext(StackGresShardedDbOps dbOps, Builder contextBuilder) {
    if (ShardedDbOpsUtil.isAlreadyCompleted(dbOps)) {
      contextBuilder.foundCoordinator(Optional.empty());
      return;
    }

    final String coordinatorClusterName = getCoordinatorClusterName(dbOps.getSpec().getSgShardedCluster());
    final Optional<StackGresCluster> foundCoordinator = clusterFinder
        .findByNameAndNamespace(
            coordinatorClusterName,
            dbOps.getMetadata().getNamespace());
    if (foundCoordinator.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresCluster.KIND + " " + coordinatorClusterName + " was not found");
    }
    contextBuilder.foundCoordinator(foundCoordinator);
  }

}
