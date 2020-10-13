/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.app;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.distributedlogs.controller.DistributedLogsReconciliationCycle;

@ApplicationScoped
public class ReconciliationClockImpl implements ReconciliationClock {

  private final ScheduledExecutorService scheduledExecutorService =
      Executors.newScheduledThreadPool(1, r -> new Thread(r, "ReconciliationShceduler"));

  private final DistributedLogsReconciliationCycle distributedLogsReconciliationCycle;

  @Inject
  public ReconciliationClockImpl(
      DistributedLogsReconciliationCycle distributedLogsReconciliationCycle) {
    this.distributedLogsReconciliationCycle = distributedLogsReconciliationCycle;
  }

  @Override
  public void start() {
    distributedLogsReconciliationCycle.start();

    scheduledExecutorService.scheduleAtFixedRate(
        this::reconcile, 0, 10, TimeUnit.SECONDS);

  }

  private void reconcile() {
    distributedLogsReconciliationCycle.reconcile();
  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdown();
    distributedLogsReconciliationCycle.stop();
  }
}
