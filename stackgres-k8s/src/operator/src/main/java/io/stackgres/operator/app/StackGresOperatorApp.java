/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.app.ReconciliationClock;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresOperatorApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresOperatorApp.class);

  private OperatorPropertyContext operatorPropertyContext;
  private OperatorWatcherHandler operatorWatchersHandler;
  private ReconciliationClock reconciliationClock;
  private OperatorBootstrap operatorBootstrap;

  void onStart(@Observes StartupEvent ev) {
    if (!operatorPropertyContext.getBoolean(OperatorProperty.DISABLE_RECONCILIATION)) {
      LOGGER.info("The reconciliation is starting...");
      operatorBootstrap.bootstrap();
      operatorWatchersHandler.startWatchers();
      reconciliationClock.start();
    }
  }

  void onStop(@Observes ShutdownEvent ev) {
    if (!operatorPropertyContext.getBoolean(OperatorProperty.DISABLE_RECONCILIATION)) {
      LOGGER.info("The reconciliation is stopping...");
      operatorWatchersHandler.stopWatchers();
      reconciliationClock.stop();
    }
  }

  @Inject
  public void setOperatorPropertyContext(OperatorPropertyContext operatorPropertyContext) {
    this.operatorPropertyContext = operatorPropertyContext;
  }

  @Inject
  public void setOperatorWatchersHandler(OperatorWatcherHandler operatorWatchersHandler) {
    this.operatorWatchersHandler = operatorWatchersHandler;
  }

  @Inject
  public void setReconciliationClock(ReconciliationClock reconciliationClock) {
    this.reconciliationClock = reconciliationClock;
  }

  @Inject
  public void setOperatorBootstrap(OperatorBootstrap operatorBootstrap) {
    this.operatorBootstrap = operatorBootstrap;
  }
}
