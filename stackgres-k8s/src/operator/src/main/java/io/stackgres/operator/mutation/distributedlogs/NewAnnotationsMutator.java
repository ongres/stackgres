/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import java.util.Optional;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NewAnnotationsMutator implements DistributedLogsMutator {

  @Override
  public StackGresDistributedLogs mutate(
      StackGresDistributedLogsReview review, StackGresDistributedLogs resource) {
    if (review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    final long versionAsNumber = StackGresVersion.getStackGresVersionAsNumber(resource);
    if (versionAsNumber < StackGresVersion.V_1_15.getVersionAsNumber()) {
      if (Optional.of(resource.getSpec())
          .map(StackGresDistributedLogsSpec::getMetadata)
          .map(StackGresClusterSpecMetadata::getAnnotations)
          .map(StackGresClusterSpecAnnotations::getPods)
          .isPresent()) {
        resource.getSpec().getMetadata().getAnnotations().setClusterPods(
            resource.getSpec().getMetadata().getAnnotations().getPods());
        resource.getSpec().getMetadata().getAnnotations().setPods(null);
      }
    }
    return resource;
  }

}
