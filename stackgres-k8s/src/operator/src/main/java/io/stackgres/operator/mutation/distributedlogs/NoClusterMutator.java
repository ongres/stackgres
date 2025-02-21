/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NoClusterMutator implements DistributedLogsMutator {

  static final long V_1_14 = StackGresVersion.V_1_14.getVersionAsNumber();

  @Override
  public StackGresDistributedLogs mutate(
      StackGresDistributedLogsReview review, StackGresDistributedLogs resource) {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      final long versionAsNumber = StackGresVersion.getStackGresVersionAsNumber(
          review.getRequest().getObject());
      if (versionAsNumber <= V_1_14) {
        if (resource.getStatus() == null) {
          resource.setStatus(new StackGresDistributedLogsStatus());
        }
        resource.getStatus().setPostgresVersion("12");
        resource.getStatus().setTimescaledbVersion("1.7.4");
      }
    }

    return resource;
  }

}
