/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import io.stackgres.common.app.AbstractReconciliationClock;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReconciliationClock extends AbstractReconciliationClock {

  private final OperatorWatchersHandler operatorWatchersHandler;
  private final Instance<AbstractReconciliator<?>> reconciliators;

  @Inject
  public ReconciliationClock(
      OperatorWatchersHandler operatorWatchersHandler,
      @Any Instance<AbstractReconciliator<?>> reconciliators) {
    this.operatorWatchersHandler = operatorWatchersHandler;
    this.reconciliators = reconciliators;
  }

  @Override
  protected void reconcile() {
    // Repair missed watch deletes before the periodic reconcile cycle runs.
    operatorWatchersHandler.resync();
    reconciliators.forEach(AbstractReconciliator::reconcileAll);
  }

}
