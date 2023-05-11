/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavor;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultPostgresVersionMutator implements ShardedClusterMutator {

  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    final String postgresVersion = resource.getSpec().getPostgres().getVersion();
    final String postgresFlavor = resource.getSpec().getPostgres().getFlavor();

    if (postgresVersion != null) {
      final String calculatedPostgresVersion = getPostgresFlavorComponent(resource)
          .get(resource).getVersion(postgresVersion);

      if (!calculatedPostgresVersion.equals(postgresVersion)) {
        resource.getSpec().getPostgres().setVersion(calculatedPostgresVersion);
      }
    } else {
      final String calculatedPostgresVersion = getPostgresFlavorComponent(resource)
          .get(resource).getVersion(StackGresComponent.LATEST);
      resource.getSpec().getPostgres().setVersion(calculatedPostgresVersion);
    }

    if (!Objects.equals(postgresFlavor, getPostgresFlavor(resource).toString())) {
      final String calculatedPostgresFlavor = getPostgresFlavor(resource).toString();
      resource.getSpec().getPostgres().setFlavor(calculatedPostgresFlavor);
    }

    return resource;
  }

}
