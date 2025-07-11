/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultDisableEnvoyMutator implements ShardedClusterMutator {

  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    if (StackGresVersion.getStackGresVersionAsNumber(resource) <= StackGresVersion.V_1_16.getVersionAsNumber()) {
      if (resource.getSpec().getCoordinator() != null) {
        if (resource.getSpec().getCoordinator().getPods() != null
            && resource.getSpec().getCoordinator().getPods().getDisableEnvoy() == null) {
          resource.getSpec().getCoordinator().getPods().setDisableEnvoy(false);
        }
      }
      if (resource.getSpec().getShards() != null) {
        if (resource.getSpec().getShards().getPods() != null
            && resource.getSpec().getShards().getPods().getDisableEnvoy() == null) {
          resource.getSpec().getShards().getPods().setDisableEnvoy(false);
        }
        if (resource.getSpec().getShards().getOverrides() != null) {
          for (var shardOverride : resource.getSpec().getShards().getOverrides()) {
            if (shardOverride.getPods() != null
                && shardOverride.getPods().getDisableEnvoy() == null) {
              shardOverride.getPods().setDisableEnvoy(false);
            }
          }
        }
      }
    } else {
      // TODO: Remove this code when 1.16 gets removed and add default for disableEnvoy fields
      //  in SGShardedCluster's CRD inside SGShardedCluster.yaml
      if (resource.getSpec().getCoordinator() != null) {
        if (resource.getSpec().getCoordinator().getPods() != null
            && resource.getSpec().getCoordinator().getPods().getDisableEnvoy() == null) {
          resource.getSpec().getCoordinator().getPods().setDisableEnvoy(true);
        }
      }
      if (resource.getSpec().getShards() != null) {
        if (resource.getSpec().getShards().getPods() != null
            && resource.getSpec().getShards().getPods().getDisableEnvoy() == null) {
          resource.getSpec().getShards().getPods().setDisableEnvoy(true);
        }
        if (resource.getSpec().getShards().getOverrides() != null) {
          for (var shardOverride : resource.getSpec().getShards().getOverrides()) {
            if (shardOverride.getPods() != null
                && shardOverride.getPods().getDisableEnvoy() == null) {
              shardOverride.getPods().setDisableEnvoy(true);
            }
          }
        }
      }
    }
    return resource;
  }

}
