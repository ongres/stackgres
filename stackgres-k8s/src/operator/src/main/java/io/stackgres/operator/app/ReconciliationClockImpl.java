/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.app.AbstractReconciliationClock;
import io.stackgres.operator.controller.ClusterReconciliationCycle;
import io.stackgres.operator.controller.DistributedLogsReconciliationCycle;

@ApplicationScoped
public class ReconciliationClockImpl extends AbstractReconciliationClock {

  private final ClusterReconciliationCycle clusterReconciliationCycle;
  private final DistributedLogsReconciliationCycle distributedLogsReconciliationCycle;

  @Inject
  public ReconciliationClockImpl(ClusterReconciliationCycle clusterReconciliationCycle,
      DistributedLogsReconciliationCycle distributedLogsReconciliationCycle) {
    this.clusterReconciliationCycle = clusterReconciliationCycle;
    this.distributedLogsReconciliationCycle = distributedLogsReconciliationCycle;
  }

  @Override
  protected void reconcile() {
    clusterReconciliationCycle.reconcile();
    distributedLogsReconciliationCycle.reconcile();
  }
}
