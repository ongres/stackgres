/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavor;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultPostgresFlavorMutator implements ClusterMutator {

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    final String calculatedPostgresFlavor = calculatePostgresFlavor(resource);
    resource.getSpec().getPostgres().setFlavor(calculatedPostgresFlavor);

    return resource;
  }

  private String calculatePostgresFlavor(StackGresCluster resource) {
    final String postgresFlavor = resource.getSpec().getPostgres().getFlavor();
    final String calculatedPostgresFlavor = getPostgresFlavor(postgresFlavor).toString();
    return calculatedPostgresFlavor;
  }

}
