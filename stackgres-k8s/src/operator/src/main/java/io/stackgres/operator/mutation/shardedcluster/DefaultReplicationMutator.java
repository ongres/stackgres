/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgcluster.StackGresReplicationMode;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterReplication;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultReplicationMutator implements ShardedClusterMutator {

  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }

    if (resource.getSpec().getReplication() == null) {
      resource.getSpec().setReplication(
          new StackGresShardedClusterReplication());
    }
    var replication = resource.getSpec().getReplication();
    if (replication.getMode() == null) {
      replication.setMode(StackGresReplicationMode.ASYNC.toString());
    }

    return resource;
  }

}
