/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProfileMutator
    implements DistributedLogsMutator {

  private static final long VERSION_1_5 = StackGresVersion.V_1_5.getVersionAsNumber();

  @Override
  public StackGresDistributedLogs mutate(
      StackGresDistributedLogsReview review, StackGresDistributedLogs resource) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
      if (version <= VERSION_1_5) {
        if (resource.getSpec().getResources() == null) {
          resource.getSpec().setResources(new StackGresClusterResources());
        }
        resource.getSpec().getResources().setDisableResourcesRequestsSplitFromTotal(true);
      }
    }
    return resource;
  }

}
