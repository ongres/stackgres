/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresCoordinatorServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresShardsServices;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultPostgresServicesMutator implements ShardedClusterMutator {

  @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
      justification = "False positive")
  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }

    if (resource.getSpec().getPostgresServices() == null) {
      resource.getSpec().setPostgresServices(new StackGresShardedClusterPostgresServices());
    }
    if (resource.getSpec().getPostgresServices().getCoordinator() == null) {
      resource.getSpec().getPostgresServices().setCoordinator(
          new StackGresShardedClusterPostgresCoordinatorServices());
    }
    if (resource.getSpec().getPostgresServices().getCoordinator().getAny() == null) {
      resource.getSpec().getPostgresServices().getCoordinator().setAny(
          new StackGresPostgresService());
    }
    if (resource.getSpec().getPostgresServices().getCoordinator().getPrimary() == null) {
      resource.getSpec().getPostgresServices().getCoordinator().setPrimary(
          new StackGresPostgresService());
    }
    if (resource.getSpec().getPostgresServices().getShards() == null) {
      resource.getSpec().getPostgresServices().setShards(
          new StackGresShardedClusterPostgresShardsServices());
    }
    if (resource.getSpec().getPostgresServices().getShards().getPrimaries() == null) {
      resource.getSpec().getPostgresServices().getShards().setPrimaries(
          new StackGresPostgresService());
    }
    setPostgresService(resource.getSpec().getPostgresServices().getCoordinator().getAny());
    setPostgresService(resource.getSpec().getPostgresServices().getCoordinator().getPrimary());
    setPostgresService(resource.getSpec().getPostgresServices().getShards().getPrimaries());

    return resource;
  }

  private void setPostgresService(StackGresPostgresService postgresService) {
    if (postgresService.getEnabled() == null) {
      postgresService.setEnabled(Boolean.TRUE);
    }

    if (postgresService.getType() == null) {
      postgresService.setType(StackGresPostgresServiceType.CLUSTER_IP.toString());
    }
  }

}
