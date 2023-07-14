/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import io.stackgres.cluster.app.StackGresClusterControllerMain.StackGresClusterControllerAppShutdownEvent;
import io.stackgres.cluster.app.StackGresClusterControllerMain.StackGresClusterControllerAppStartupEvent;
import io.stackgres.common.app.ReconciliationClock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresClusterControllerApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresClusterControllerApp.class);

  private ClusterControllerWatcherHandler operatorWatchersHandler;
  private ReconciliationClock reconciliationClock;
  private ClusterControllerBootstrap operatorBootstrap;

  void onStart(@Observes StackGresClusterControllerAppStartupEvent ev) {
    operatorBootstrap.bootstrap();
    operatorWatchersHandler.startWatchers();
    reconciliationClock.start();
  }

  void onStop(@Observes StackGresClusterControllerAppShutdownEvent ev) {
    LOGGER.info("The application is stopping...");
    operatorWatchersHandler.stopWatchers();
    reconciliationClock.stop();
  }

  @Inject
  public void setOperatorWatchersHandler(
      ClusterControllerWatcherHandler operatorWatchersHandler) {
    this.operatorWatchersHandler = operatorWatchersHandler;
  }

  @Inject
  public void setReconciliationClock(ReconciliationClock reconciliationClock) {
    this.reconciliationClock = reconciliationClock;
  }

  @Inject
  public void setOperatorBootstrap(ClusterControllerBootstrap operatorBootstrap) {
    this.operatorBootstrap = operatorBootstrap;
  }
}
