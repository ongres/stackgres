/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.stream.Collectors;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterConciliator extends Conciliator<StackGresCluster> {

  private final ClusterStatusManager statusManager;

  @Inject
  public ClusterConciliator(ClusterStatusManager statusManager) {
    this.statusManager = statusManager;
  }

  @Override
  public ReconciliationResult evalReconciliationState(StackGresCluster config) {
    final ReconciliationResult reconciliationResult = super.evalReconciliationState(config);

    if (statusManager.isPendingRestart(config)) {
      reconciliationResult.setDeletions(reconciliationResult.getDeletions().stream()
          .filter(ReconciliationUtil::isResourceReconciliationNotPausedUntilRestart)
          .collect(Collectors.toUnmodifiableList()));

      reconciliationResult.setPatches(reconciliationResult.getPatches().stream()
          .filter(tuple -> ReconciliationUtil
              .isResourceReconciliationNotPausedUntilRestart(tuple.v2))
          .collect(Collectors.toUnmodifiableList()));
    }

    return reconciliationResult;
  }
}
