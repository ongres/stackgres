/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.cluster.controller.ClusterControllerReconciliationCycle;
import io.stackgres.common.app.AbstractReconciliationClock;

@ApplicationScoped
public class ReconciliationClockImpl extends AbstractReconciliationClock {

  private final ClusterControllerReconciliationCycle clusterReconciliationCycle;

  @Inject
  public ReconciliationClockImpl(
      ClusterControllerReconciliationCycle clusterReconciliationCycle) {
    this.clusterReconciliationCycle = clusterReconciliationCycle;
  }

  @Override
  protected void reconcile() {
    clusterReconciliationCycle.reconcileAll();
  }
}
