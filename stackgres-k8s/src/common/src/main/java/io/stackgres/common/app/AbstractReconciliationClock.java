/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.app;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractReconciliationClock implements ReconciliationClock {

  private final ScheduledExecutorService scheduledExecutorService =
      Executors.newScheduledThreadPool(1, r -> new Thread(r, "ReconciliationShceduler"));

  @Override
  public void start() {
    scheduledExecutorService.scheduleAtFixedRate(
        this::reconcile, 0, 10, TimeUnit.SECONDS);
  }

  protected abstract void reconcile();

  @Override
  public void stop() {
    scheduledExecutorService.shutdown();
  }
}
