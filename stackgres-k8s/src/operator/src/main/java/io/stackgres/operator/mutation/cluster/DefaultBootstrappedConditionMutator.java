/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultBootstrappedConditionMutator
    extends ConditionUpdater<StackGresCluster, Condition>
    implements ClusterMutator {

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    if (StackGresVersion.getStackGresVersionAsNumber(resource) <= StackGresVersion.V_1_17.getVersionAsNumber()) {
      boolean isPlatformSet = resource.getStatus() != null
          && resource.getStatus().getArch() != null
          && resource.getStatus().getOs() != null;
      if (isPlatformSet) {
        updateCondition(getClusterBootstrapped(), resource);
      }
    }
    return resource;
  }

  private Condition getClusterBootstrapped() {
    return ClusterStatusCondition.CLUSTER_BOOTSTRAPPED.getCondition();
  }

  @Override
  protected List<Condition> getConditions(
      StackGresCluster source) {
    return Optional.ofNullable(source.getStatus())
        .map(StackGresClusterStatus::getConditions)
        .orElse(List.of());
  }

  @Override
  protected void setConditions(
      StackGresCluster source,
      List<Condition> conditions) {
    if (source.getStatus() == null) {
      source.setStatus(new StackGresClusterStatus());
    }
    source.getStatus().setConditions(conditions);
  }

}
