/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.app;

import io.stackgres.common.app.AbstractReconciliationClock;
import io.stackgres.distributedlogs.controller.DistributedLogsControllerReconciliationCycle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReconciliationClockImpl extends AbstractReconciliationClock {

  private final DistributedLogsControllerReconciliationCycle distributedLogsReconciliationCycle;

  @Inject
  public ReconciliationClockImpl(
      DistributedLogsControllerReconciliationCycle distributedLogsReconciliationCycle) {
    this.distributedLogsReconciliationCycle = distributedLogsReconciliationCycle;
  }

  @Override
  protected void reconcile() {
    distributedLogsReconciliationCycle.reconcileAll();
  }
}
