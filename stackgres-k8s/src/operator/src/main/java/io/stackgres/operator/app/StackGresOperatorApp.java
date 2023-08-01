/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.app.ReconciliationClock;
import io.stackgres.operator.conciliation.OperatorLockHolder;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresOperatorApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresOperatorApp.class);

  private final OperatorPropertyContext operatorPropertyContext;
  private final OperatorWatcherHandler operatorWatchersHandler;
  private final ReconciliationClock reconciliationClock;
  private final OperatorBootstrap operatorBootstrap;
  private final OperatorLockHolder operatorLockHolder;

  @Inject
  public StackGresOperatorApp(
      OperatorPropertyContext operatorPropertyContext,
      OperatorWatcherHandler operatorWatchersHandler,
      ReconciliationClock reconciliationClock,
      OperatorBootstrap operatorBootstrap,
      OperatorLockHolder operatorLockHolder) {
    this.operatorPropertyContext = operatorPropertyContext;
    this.operatorWatchersHandler = operatorWatchersHandler;
    this.reconciliationClock = reconciliationClock;
    this.operatorBootstrap = operatorBootstrap;
    this.operatorLockHolder = operatorLockHolder;
  }

  void onStart(@Observes StartupEvent ev) {
    if (!operatorPropertyContext.getBoolean(OperatorProperty.DISABLE_RECONCILIATION)) {
      LOGGER.info("The reconciliation is starting...");
      operatorBootstrap.bootstrap();
      operatorWatchersHandler.startWatchers();
      reconciliationClock.start();
      operatorLockHolder.start();
    }
  }

  void onStop(@Observes ShutdownEvent ev) {
    if (!operatorPropertyContext.getBoolean(OperatorProperty.DISABLE_RECONCILIATION)) {
      LOGGER.info("The reconciliation is stopping...");
      operatorWatchersHandler.stopWatchers();
      reconciliationClock.stop();
      operatorLockHolder.stop();
    }
  }

}
