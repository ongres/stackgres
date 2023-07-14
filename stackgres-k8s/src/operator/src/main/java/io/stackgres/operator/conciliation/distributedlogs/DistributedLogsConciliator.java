/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.stream.Collectors;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsConciliator extends Conciliator<StackGresDistributedLogs> {

  private final DistributedLogsStatusManager distributedLogsStatusManager;

  @Inject
  public DistributedLogsConciliator(DistributedLogsStatusManager distributedLogsStatusManager) {
    this.distributedLogsStatusManager = distributedLogsStatusManager;
  }

  @Override
  public ReconciliationResult evalReconciliationState(StackGresDistributedLogs config) {
    final ReconciliationResult reconciliationResult = super.evalReconciliationState(config);

    if (distributedLogsStatusManager.isPendingRestart(config)) {
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
