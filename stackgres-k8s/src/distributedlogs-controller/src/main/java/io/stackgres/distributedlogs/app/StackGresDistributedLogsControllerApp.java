/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.app;

import io.stackgres.common.app.ReconciliationClock;
import io.stackgres.distributedlogs.app.StackGresDistributedLogsControllerMain.StackGresDistributedLogsControllerAppShutdownEvent;
import io.stackgres.distributedlogs.app.StackGresDistributedLogsControllerMain.StackGresDistributedLogsControllerAppStartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresDistributedLogsControllerApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      StackGresDistributedLogsControllerApp.class);

  private DistributedLogsControllerWatcherHandler operatorWatchersHandler;
  private ReconciliationClock reconciliationClock;
  private DistributedLogsControllerBootstrap operatorBootstrap;

  void onStart(@Observes StackGresDistributedLogsControllerAppStartupEvent ev) {
    operatorBootstrap.bootstrap();
    operatorWatchersHandler.startWatchers();
    reconciliationClock.start();
  }

  void onStop(@Observes StackGresDistributedLogsControllerAppShutdownEvent ev) {
    LOGGER.info("The application is stopping...");
    operatorWatchersHandler.stopWatchers();
    reconciliationClock.stop();
  }

  @Inject
  public void setOperatorWatchersHandler(
      DistributedLogsControllerWatcherHandler operatorWatchersHandler) {
    this.operatorWatchersHandler = operatorWatchersHandler;
  }

  @Inject
  public void setReconciliationClock(ReconciliationClock reconciliationClock) {
    this.reconciliationClock = reconciliationClock;
  }

  @Inject
  public void setOperatorBootstrap(DistributedLogsControllerBootstrap operatorBootstrap) {
    this.operatorBootstrap = operatorBootstrap;
  }
}
