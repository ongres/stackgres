/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.AbstractResourceHandler;

public abstract class AbstractPausableResourceHandler<
      T extends StackGresClusterContext>
    extends AbstractResourceHandler<T> {

  @Override
  public boolean skipUpdate(T context, HasMetadata existingResource, HasMetadata requiredResource) {
    return isReconciliationPause(context, existingResource);
  }

  @Override
  public boolean skipDeletion(T context, HasMetadata existingResource) {
    return isReconciliationPause(context, existingResource);
  }

  private boolean isReconciliationPause(T context, HasMetadata existingResource) {
    Optional<Boolean> reconciliationPause =
        Optional.ofNullable(existingResource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(
            StackGresContext.RECONCILIATION_PAUSE_KEY))
        .map(Boolean::valueOf);
    Optional<Boolean> reconciliationPauseUntilRestart =
        Optional.ofNullable(existingResource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(
            StackGresContext.RECONCILIATION_PAUSE_UNTIL_RESTART_KEY))
        .map(Boolean::valueOf);
    Boolean clusterPendingRestart = Optional.ofNullable(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getConditions)
        .orElse(ImmutableList.of())
        .stream()
        .anyMatch(condition -> condition.getType().equals(
            ClusterStatusCondition.Type.PENDING_RESTART.getType())
            && condition.getStatus().equals(
                ClusterStatusCondition.Status.TRUE.getStatus()));
    return reconciliationPause.orElse(false)
        || (reconciliationPauseUntilRestart.orElse(false) // NOPMD
            && clusterPendingRestart);
  }

}
