/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import java.time.Duration;
import java.time.Instant;

import io.stackgres.cluster.controller.ManagedSqlCronScheduler;
import io.stackgres.cluster.controller.ManagedSqlReconciliationCycle;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.app.AbstractReconciliationClock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Check each second if the managed SQL has to be reconciled: when an SGScript entry that sets the
 * field {@code cron} is due or when the reconciliation period elapsed since the last time.
 */
@ApplicationScoped
public class ManagedSqlReconciliationClock extends AbstractReconciliationClock {

  private final ManagedSqlReconciliationCycle managedSqlReconciliationCycle;
  private final ManagedSqlCronScheduler managedSqlCronScheduler;
  private final Duration reconciliationPeriod;
  private Instant lastReconciliation;

  @Inject
  public ManagedSqlReconciliationClock(
      ManagedSqlReconciliationCycle managedSqlReconciliationCycle,
      ManagedSqlCronScheduler managedSqlCronScheduler) {
    super("ManagedSqlReconciliationScheduler");
    this.managedSqlReconciliationCycle = managedSqlReconciliationCycle;
    this.managedSqlCronScheduler = managedSqlCronScheduler;
    this.reconciliationPeriod = Duration.ofSeconds(OperatorProperty.RECONCILIATION_PERIOD
        .get()
        .map(Integer::valueOf)
        .orElse(60));
  }

  @Override
  protected int getPeriod() {
    return 1;
  }

  @Override
  protected void reconcile() {
    final Instant now = managedSqlCronScheduler.now();
    if (managedSqlCronScheduler.isAnyExecutionDue()
        || lastReconciliation == null
        || !now.isBefore(lastReconciliation.plus(reconciliationPeriod))) {
      lastReconciliation = now;
      managedSqlCronScheduler.unschedule();
      managedSqlReconciliationCycle.reconcileAll();
    }
  }

}
