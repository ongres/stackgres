/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.Arrays;
import java.util.Optional;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterUpdateStrategy;
import io.stackgres.common.crd.sgcluster.StackGresClusterUpdateStrategyType;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UpdateStrategyMutator implements ClusterMutator {

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    //TODO: remove this mutator when 1.18 reach EOL
    if (StackGresVersion.V_1_18.getVersion() != null) {
      if (Arrays.asList(StackGresClusterUpdateStrategyType.values())
          .stream()
          .map(Object::toString)
          .noneMatch(
              Optional.of(resource)
              .map(StackGresCluster::getSpec)
              .map(StackGresClusterSpec::getPods)
              .map(StackGresClusterPods::getUpdateStrategy)
              .map(StackGresClusterUpdateStrategy::getType)
              .orElse("")::equals)) {
        if (resource.getSpec() == null) {
          resource.setSpec(new StackGresClusterSpec());
        }
        if (resource.getSpec().getPods() == null) {
          resource.getSpec().setPods(new StackGresClusterPods());
        }
        if (resource.getSpec().getPods().getUpdateStrategy() == null) {
          resource.getSpec().getPods().setUpdateStrategy(new StackGresClusterUpdateStrategy());
        }
        resource.getSpec().getPods().getUpdateStrategy().setType(
            StackGresClusterUpdateStrategyType.ONLY_DB_OPS.toString());
      }
    }

    return resource;
  }

}
