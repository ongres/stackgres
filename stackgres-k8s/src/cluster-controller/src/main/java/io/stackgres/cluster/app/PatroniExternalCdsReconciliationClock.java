/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import io.stackgres.cluster.controller.PatroniExternalCdsControllerReconciliationCycle;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.app.AbstractReconciliationClock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PatroniExternalCdsReconciliationClock extends AbstractReconciliationClock {

  private final PatroniExternalCdsControllerReconciliationCycle patroniReconciliationCycle;

  @Inject
  public PatroniExternalCdsReconciliationClock(
      PatroniExternalCdsControllerReconciliationCycle patroniReconciliationCycle) {
    super("PatroniReconciliationScheduler");
    this.patroniReconciliationCycle = patroniReconciliationCycle;
  }

  @Override
  protected int getPeriod() {
    return OperatorProperty.PATRONI_RECONCILIATION_PERIOD
        .get()
        .map(Integer::valueOf)
        .orElse(10);
  }

  @Override
  protected void reconcile() {
    patroniReconciliationCycle.reconcileAll();
  }

}
