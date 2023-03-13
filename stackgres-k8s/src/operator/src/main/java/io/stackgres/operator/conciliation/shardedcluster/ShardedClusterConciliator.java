/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliationUtil;

@ApplicationScoped
public class ShardedClusterConciliator extends Conciliator<StackGresShardedCluster> {

  private final ShardedClusterStatusManager statusManager;

  @Inject
  public ShardedClusterConciliator(ShardedClusterStatusManager statusManager) {
    this.statusManager = statusManager;
  }

  @Override
  public ReconciliationResult evalReconciliationState(StackGresShardedCluster config) {
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
