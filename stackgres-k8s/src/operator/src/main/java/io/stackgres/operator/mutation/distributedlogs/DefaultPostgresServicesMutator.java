/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServices;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultPostgresServicesMutator implements DistributedLogsMutator {

  @Override
  public StackGresDistributedLogs mutate(
      StackGresDistributedLogsReview review, StackGresDistributedLogs resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    mutatePgServices(resource);
    return resource;
  }

  private void mutatePgServices(StackGresDistributedLogs resource) {
    if (resource.getSpec().getPostgresServices() == null) {
      resource.getSpec().setPostgresServices(new StackGresPostgresServices());
    }
    setPgPrimaryService(resource.getSpec().getPostgresServices());
    setPgReplicasService(resource.getSpec().getPostgresServices());
  }

  private void setPgPrimaryService(StackGresPostgresServices postgresServices) {
    if (postgresServices.getPrimary() == null) {
      postgresServices.setPrimary(new StackGresPostgresService());
    }
    postgresServices.getPrimary().setEnabled(null);
    if (postgresServices.getPrimary().getType() == null) {
      postgresServices.getPrimary()
          .setType(StackGresPostgresServiceType.CLUSTER_IP.toString());
    }
  }

  private void setPgReplicasService(StackGresPostgresServices postgresServices) {
    if (postgresServices.getReplicas() == null) {
      postgresServices.setReplicas(new StackGresPostgresService());
    }

    if (postgresServices.getReplicas().getEnabled() == null) {
      postgresServices.getReplicas().setEnabled(Boolean.TRUE);
    }
    if (postgresServices.getReplicas().getType() == null) {
      postgresServices.getReplicas()
          .setType(StackGresPostgresServiceType.CLUSTER_IP.toString());
    }
  }

}
