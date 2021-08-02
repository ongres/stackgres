/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.app.AbstractReconciliationClock;
import io.stackgres.operator.conciliation.cluster.ClusterReconciliator;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsReconciliator;

@ApplicationScoped
public class ReconciliationClockImpl extends AbstractReconciliationClock {

  private final ClusterReconciliator clusterReconciliationCycle;
  private final DistributedLogsReconciliator distributedLogsReconciliator;

  @Inject
  public ReconciliationClockImpl(ClusterReconciliator clusterReconciliationCycle,
                                 DistributedLogsReconciliator distributedLogsConciliator) {
    this.clusterReconciliationCycle = clusterReconciliationCycle;
    this.distributedLogsReconciliator = distributedLogsConciliator;
  }

  @Override
  protected void reconcile() {
    clusterReconciliationCycle.reconcile();
    distributedLogsReconciliator.reconcile();
  }

}
