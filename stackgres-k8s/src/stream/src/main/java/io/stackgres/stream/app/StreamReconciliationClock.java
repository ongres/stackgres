/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.app;

import io.stackgres.common.app.AbstractReconciliationClock;
import io.stackgres.stream.controller.StreamReconciliationCycle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamReconciliationClock extends AbstractReconciliationClock {

  private final StreamReconciliationCycle streamReconciliationCycle;

  @Inject
  public StreamReconciliationClock(
      StreamReconciliationCycle streamReconciliationCycle) {
    this.streamReconciliationCycle = streamReconciliationCycle;
  }

  @Override
  protected void reconcile() {
    streamReconciliationCycle.reconcileAll();
  }

}
