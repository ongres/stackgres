/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.app;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.stackgres.common.OperatorProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReconciliationClock implements ReconciliationClock {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractReconciliationClock.class);

  private final ScheduledExecutorService scheduledExecutorService =
      Executors.newScheduledThreadPool(1, r -> new Thread(r, "ReconciliationScheduler"));

  @Override
  public void start() {
    scheduledExecutorService.scheduleAtFixedRate(
        this::safeReconcile, 0, getPeriod(), getTimeUnit());
  }

  private void safeReconcile() {
    try {
      reconcile();
    } catch (Exception ex) {
      LOGGER.error("Error occurred during scheduled reconciliation.", ex);
    }
  }

  protected abstract void reconcile();

  protected int getPeriod() {
    return OperatorProperty.RECONCILIATION_PERIOD
        .get()
        .map(Integer::valueOf)
        .orElse(60);
  }

  protected TimeUnit getTimeUnit() {
    return TimeUnit.SECONDS;
  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdown();
  }
}
