/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultPostgresSslMutator implements ShardedClusterMutator {

  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    if (resource.getSpec().getPostgres() != null) {
      if (resource.getSpec().getPostgres().getSsl() == null) {
        resource.getSpec().getPostgres().setSsl(new StackGresClusterSsl());
      }
      if (resource.getSpec().getPostgres().getSsl().getEnabled() == null) {
        resource.getSpec().getPostgres().getSsl().setEnabled(true);
      }
    }
    return resource;
  }

}
