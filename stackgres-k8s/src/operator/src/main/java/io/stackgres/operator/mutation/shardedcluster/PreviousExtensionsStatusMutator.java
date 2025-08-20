/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import java.util.Optional;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PreviousExtensionsStatusMutator implements ShardedClusterMutator {

  @Override
  public StackGresShardedCluster mutate(StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    if (StackGresVersion.getStackGresVersionAsNumber(resource) <= StackGresVersion.V_1_17.getVersionAsNumber()) {
      if (resource.getStatus() == null) {
        resource.setStatus(new StackGresShardedClusterStatus());
      }
      Optional.ofNullable(resource.getStatus())
          .map(StackGresShardedClusterStatus::getToInstallPostgresExtensions)
          .ifPresent(extensions -> {
            resource.getStatus().setToInstallPostgresExtensions(null);
            resource.getStatus().setExtensions(extensions);
          });
    }
    return resource;
  }

}
