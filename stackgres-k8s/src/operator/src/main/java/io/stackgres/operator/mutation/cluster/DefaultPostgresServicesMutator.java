/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultPostgresServicesMutator implements ClusterMutator {

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }

    if (resource.getSpec().getPostgresServices() == null) {
      resource.getSpec().setPostgresServices(new StackGresPostgresServices());
    }
    if (resource.getSpec().getPostgresServices().getPrimary() == null) {
      resource.getSpec().getPostgresServices().setPrimary(new StackGresPostgresService());
    }
    if (resource.getSpec().getPostgresServices().getReplicas() == null) {
      resource.getSpec().getPostgresServices().setReplicas(new StackGresPostgresService());
    }
    setPostgresService(resource.getSpec().getPostgresServices().getPrimary());
    setPostgresService(resource.getSpec().getPostgresServices().getReplicas());

    return resource;
  }

  private void setPostgresService(
      StackGresPostgresService postgresService) {
    if (postgresService.getEnabled() == null) {
      postgresService.setEnabled(Boolean.TRUE);
    }

    if (postgresService.getType() == null) {
      postgresService.setType(StackGresPostgresServiceType.CLUSTER_IP.toString());
    }
  }

}
