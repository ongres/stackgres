/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.app.AbstractReconciliationClock;
import io.stackgres.operator.conciliation.AbstractReconciliator;

@ApplicationScoped
public class ReconciliationClockImpl extends AbstractReconciliationClock {

  private final Instance<AbstractReconciliator<?>> reconciliators;

  @Inject
  public ReconciliationClockImpl(@Any Instance<AbstractReconciliator<?>> reconciliators) {
    this.reconciliators = reconciliators;
  }

  @Override
  protected void reconcile() {
    reconciliators.forEach(AbstractReconciliator::reconcile);
  }

}
