/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.Optional;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PreviousExtensionsStatusMutator implements ClusterMutator {

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    if (StackGresVersion.getStackGresVersionAsNumber(resource) <= StackGresVersion.V_1_17.getVersionAsNumber()) {
      if (resource.getStatus() == null) {
        resource.setStatus(new StackGresClusterStatus());
      }
      Optional.of(resource.getSpec())
          .map(StackGresClusterSpec::getToInstallPostgresExtensions)
          .ifPresent(extensions -> {
            resource.getSpec().setToInstallPostgresExtensions(null);
            resource.getStatus().setExtensions(extensions);
          });
    }
    return resource;
  }

}
