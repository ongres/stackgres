/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavor;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultPostgresVersionMutator implements ClusterMutator {

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    final String calculatedPostgresFlavor = calculatePostgresFlavor(resource);
    resource.getSpec().getPostgres().setFlavor(calculatedPostgresFlavor);
    final String calculatedPostgresVersion = calculatePostgresVersion(resource, calculatedPostgresFlavor);
    resource.getSpec().getPostgres().setVersion(calculatedPostgresVersion);

    return resource;
  }

  private String calculatePostgresVersion(StackGresCluster resource, final String calculatedPostgresFlavor) {
    final String calculatedPostgresVersion;
    final String postgresVersion = resource.getSpec().getPostgres().getVersion();
    if (postgresVersion != null) {
      calculatedPostgresVersion = getPostgresFlavorComponent(calculatedPostgresFlavor)
          .get(resource).getVersion(postgresVersion);

    } else {
      calculatedPostgresVersion = getPostgresFlavorComponent(calculatedPostgresFlavor)
          .get(resource).getVersion(StackGresComponent.LATEST);
    }
    return calculatedPostgresVersion;
  }

  private String calculatePostgresFlavor(StackGresCluster resource) {
    final String postgresFlavor = resource.getSpec().getPostgres().getFlavor();
    final String calculatedPostgresFlavor = getPostgresFlavor(postgresFlavor).toString();
    return calculatedPostgresFlavor;
  }

}
