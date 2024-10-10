/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import io.stackgres.common.crd.sgcluster.StackGresClusterRegistry;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Default {@code spec.configurations.registry.enabled} to {@code true} for new SGShardedClusters
 * and to {@code false} for SGShardedClusters created before the field existed, see the SGCluster
 * {@link io.stackgres.operator.mutation.cluster.RegistryMutator}.
 */
@ApplicationScoped
public class RegistryMutator implements ShardedClusterMutator {

  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    if (resource.getSpec() == null) {
      resource.setSpec(new StackGresShardedClusterSpec());
    }
    if (resource.getSpec().getConfigurations() == null) {
      resource.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
    }
    if (resource.getSpec().getConfigurations().getRegistry() == null) {
      resource.getSpec().getConfigurations().setRegistry(new StackGresClusterRegistry());
    }
    if (resource.getSpec().getConfigurations().getRegistry().getEnabled() == null) {
      resource.getSpec().getConfigurations().getRegistry().setEnabled(
          review.getRequest().getOperation() == Operation.CREATE);
    }
    return resource;
  }

}
