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

  private final Instance<AbstractReconciliator<?, ?>> reconciliators;

  @Inject
  public ReconciliationClock(@Any Instance<AbstractReconciliator<?, ?>> reconciliators) {
    this.reconciliators = reconciliators;
  }

  @Override
  protected void reconcile() {
    reconciliators.forEach(AbstractReconciliator::reconcileAll);
  }

}
