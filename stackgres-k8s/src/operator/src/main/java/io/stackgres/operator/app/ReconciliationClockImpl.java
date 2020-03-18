/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.controller.ClusterReconciliationCycle;

@ApplicationScoped
public class ReconciliationClockImpl implements ReconciliationClock {

  private final ScheduledExecutorService scheduledExecutorService =
      Executors.newScheduledThreadPool(1, r -> new Thread(r, "ClusterControllerShceduler"));

  private ClusterReconciliationCycle clusterReconciliationCycle;

  @Inject
  public ReconciliationClockImpl(ClusterReconciliationCycle clusterReconciliationCycle) {
    this.clusterReconciliationCycle = clusterReconciliationCycle;
  }

  @Override
  public void start() {
    clusterReconciliationCycle.start();

    scheduledExecutorService.scheduleAtFixedRate(
        () -> clusterReconciliationCycle.reconcile(), 0, 10, TimeUnit.SECONDS);

  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdown();
    clusterReconciliationCycle.stop();
  }
}
