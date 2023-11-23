/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProfileMutator
    implements ClusterMutator {

  private static final long VERSION_1_5 = StackGresVersion.V_1_5.getVersionAsNumber();

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
      if (version <= VERSION_1_5) {
        if (resource.getSpec().getPods().getResources() == null) {
          resource.getSpec().getPods().setResources(new StackGresClusterResources());
        }
        resource.getSpec().getPods().getResources().setDisableResourcesRequestsSplitFromTotal(true);
      }
    }
    return resource;
  }

}
