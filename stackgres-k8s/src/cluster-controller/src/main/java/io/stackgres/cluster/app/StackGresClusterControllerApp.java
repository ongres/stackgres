/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.app;

import io.stackgres.cluster.app.StackGresClusterControllerMain.StackGresClusterControllerAppShutdownEvent;
import io.stackgres.cluster.app.StackGresClusterControllerMain.StackGresClusterControllerAppStartupEvent;
import io.stackgres.common.ClusterControllerProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresClusterControllerApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresClusterControllerApp.class);

  private ClusterControllerWatchersHandler operatorWatchersHandler;
  private ClusterControllerReconciliationClock reconciliationClock;
  private PatroniExternalCdsReconciliationClock patroniReconciliationClock;
  private ClusterControllerBootstrap operatorBootstrap;

  void onStart(@Observes StackGresClusterControllerAppStartupEvent ev) {
    operatorBootstrap.bootstrap();
    operatorWatchersHandler.startWatchers();
    reconciliationClock.start();
    if (ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_PATRONI_LABELS.getBoolean()) {
      patroniReconciliationClock.start();
    }
  }

  void onStop(@Observes StackGresClusterControllerAppShutdownEvent ev) {
    LOGGER.info("The application is stopping...");
    operatorWatchersHandler.stopWatchers();
    reconciliationClock.stop();
    if (ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_PATRONI_LABELS.getBoolean()) {
      patroniReconciliationClock.stop();
    }
  }

  @Inject
  public void setOperatorWatchersHandler(
      ClusterControllerWatchersHandler operatorWatchersHandler) {
    this.operatorWatchersHandler = operatorWatchersHandler;
  }

  @Inject
  public void setClusterControllerReconciliationClock(ClusterControllerReconciliationClock reconciliationClock) {
    this.reconciliationClock = reconciliationClock;
  }

  @Inject
  public void setPatroniExternalCdsReconciliationClock(
      PatroniExternalCdsReconciliationClock patroniReconciliationClock) {
    this.patroniReconciliationClock = patroniReconciliationClock;
  }

  @Inject
  public void setOperatorBootstrap(ClusterControllerBootstrap operatorBootstrap) {
    this.operatorBootstrap = operatorBootstrap;
  }
}
