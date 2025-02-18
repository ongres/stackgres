/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops.context;

import java.util.Optional;

import io.stackgres.common.ShardedDbOpsUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedDbOpsClusterContextAppender
    extends ContextAppender<StackGresShardedDbOps, Builder> {

  private final CustomResourceFinder<StackGresShardedCluster> clusterFinder;
  private final ShardedDbOpsClusterInstanceProfileContextAppender shardedDbOpsClusterInstanceProfileContextAppender;
  private final ShardedDbOpsClusterMajorVersionUpgradeContextAppender
      shardedDbOpsClusterMajorVersionUpgradeContextAppender;

  public ShardedDbOpsClusterContextAppender(
      CustomResourceFinder<StackGresShardedCluster> clusterFinder,
      ShardedDbOpsClusterInstanceProfileContextAppender shardedDbOpsClusterInstanceProfileContextAppender,
      ShardedDbOpsClusterMajorVersionUpgradeContextAppender shardedDbOpsClusterMajorVersionUpgradeContextAppender) {
    this.clusterFinder = clusterFinder;
    this.shardedDbOpsClusterInstanceProfileContextAppender = shardedDbOpsClusterInstanceProfileContextAppender;
    this.shardedDbOpsClusterMajorVersionUpgradeContextAppender = shardedDbOpsClusterMajorVersionUpgradeContextAppender;
  }

  @Override
  public void appendContext(StackGresShardedDbOps dbOps, Builder contextBuilder) {
    if (ShardedDbOpsUtil.isAlreadyCompleted(dbOps)) {
      contextBuilder.foundShardedCluster(Optional.empty());
      return;
    }

    final Optional<StackGresShardedCluster> foundCluster = clusterFinder
        .findByNameAndNamespace(
            dbOps.getSpec().getSgShardedCluster(),
            dbOps.getMetadata().getNamespace());

    contextBuilder.foundShardedCluster(foundCluster);

    if (foundCluster.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresShardedCluster.KIND + " " + dbOps.getSpec().getSgShardedCluster() + " was not found");
    }
    final StackGresShardedCluster cluster = foundCluster.get();
    shardedDbOpsClusterInstanceProfileContextAppender.appendContext(cluster, contextBuilder);
    if (dbOps.getSpec().isOpMajorVersionUpgrade()) {
      shardedDbOpsClusterMajorVersionUpgradeContextAppender.appendContext(dbOps, cluster, contextBuilder);
    }
  }

}
