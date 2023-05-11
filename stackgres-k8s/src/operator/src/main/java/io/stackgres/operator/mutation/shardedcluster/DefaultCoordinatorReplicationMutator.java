/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgcluster.StackGresReplicationMode;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterReplication;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultCoordinatorReplicationMutator implements ShardedClusterMutator {

  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }

    if (resource.getSpec().getCoordinator() == null) {
      resource.getSpec().setCoordinator(new StackGresShardedClusterCoordinator());
    }
    if (resource.getSpec().getCoordinator().getReplicationForCoordinator() == null) {
      resource.getSpec().getCoordinator().setReplicationForCoordinator(
          new StackGresShardedClusterReplication());
    }
    var replication = resource.getSpec().getCoordinator().getReplicationForCoordinator();
    if (replication.getMode() == null) {
      replication.setMode(StackGresReplicationMode.SYNC_ALL.toString());
    }

    return resource;
  }

}
