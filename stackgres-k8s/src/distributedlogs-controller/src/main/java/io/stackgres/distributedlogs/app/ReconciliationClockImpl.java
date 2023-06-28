/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.app;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.app.AbstractReconciliationClock;
import io.stackgres.distributedlogs.controller.DistributedLogsControllerReconciliationCycle;

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
