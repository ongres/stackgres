/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProfileMutator
    implements ShardedClusterMutator {

  private static final long VERSION_1_5 = StackGresVersion.V_1_5.getVersionAsNumber();

  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
      if (version <= VERSION_1_5) {
        if (resource.getSpec().getCoordinator().getPods().getResources() == null) {
          resource.getSpec().getCoordinator().getPods()
              .setResources(new StackGresClusterResources());
        }
        resource.getSpec().getCoordinator().getPods().getResources()
            .setDisableResourcesRequestsSplitFromTotal(true);
        if (resource.getSpec().getShards().getPods().getResources() == null) {
          resource.getSpec().getShards().getPods()
              .setResources(new StackGresClusterResources());
        }
        resource.getSpec().getShards().getPods().getResources()
            .setDisableResourcesRequestsSplitFromTotal(true);
        Optional.of(resource.getSpec().getShards())
            .map(StackGresShardedClusterShards::getOverrides)
            .stream()
            .flatMap(List::stream)
            .forEach(shardOverride -> {
              if (shardOverride.getPodsForShards() != null) {
                if (shardOverride.getPodsForShards().getResources() == null) {
                  shardOverride.getPodsForShards().setResources(new StackGresClusterResources());
                }
                shardOverride.getPodsForShards().getResources()
                    .setDisableResourcesRequestsSplitFromTotal(true);
              }
            });
      }
    }
    return resource;
  }

}
