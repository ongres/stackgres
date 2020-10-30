/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.app;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.app.ReconciliationClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresDistributedLogsApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresDistributedLogsApp.class);

  private DistributedLogsControllerWatcherHandler operatorWatchersHandler;
  private ReconciliationClock reconciliationClock;
  private DistributedLogsControllerBootstrap operatorBootstrap;

  void onStart(@Observes StartupEvent ev) {
    operatorBootstrap.bootstrap();
    operatorWatchersHandler.startWatchers();
    reconciliationClock.start();
  }

  void onStop(@Observes ShutdownEvent ev) {
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
