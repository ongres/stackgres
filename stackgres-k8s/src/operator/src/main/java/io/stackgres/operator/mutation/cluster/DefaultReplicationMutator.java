/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresMainReplicationRole;
import io.stackgres.common.crd.sgcluster.StackGresReplicationMode;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DefaultReplicationMutator implements ClusterMutator {

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }

    if (resource.getSpec().getReplication() == null) {
      resource.getSpec().setReplication(new StackGresClusterReplication());
    }
    var replication = resource.getSpec().getReplication();
    if (replication.getMode() == null) {
      replication.setMode(StackGresReplicationMode.ASYNC.toString());
    }
    if (replication.getRole() == null) {
      replication.setRole(StackGresMainReplicationRole.HA_READ.toString());
    }
    if (replication.getSyncInstances() == null && replication.isSynchronousMode()) {
      replication.setSyncInstances(1);
    }
    if (replication.getGroups() != null) {
      Seq.seq(replication.getGroups()).zipWithIndex().forEach(group -> {
        if (group.v1.getName() == null) {
          group.v1.setName("group-" + (group.v2 + 1));
        }
        if (group.v1.getRole() == null) {
          group.v1.setRole(StackGresMainReplicationRole.HA_READ.toString());
        }
      });
    }

    return resource;
  }

}
