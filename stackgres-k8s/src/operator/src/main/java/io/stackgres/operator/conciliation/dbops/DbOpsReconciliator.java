/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgcluster.DbOpsEventReason;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsCondition;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.PatchResumer;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.ComparisonDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DbOpsReconciliator
    extends AbstractReconciliator<StackGresDbOps> {

  public DbOpsReconciliator() {
    super(StackGresDbOps.KIND);
  }

  private EventEmitter<StackGresDbOps> eventController;

  private PatchResumer<StackGresDbOps> patchResumer;

  private StatusManager<StackGresDbOps, StackGresDbOpsCondition> statusManager;

  private CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  public void onPreReconciliation(StackGresDbOps config) {
  }

  @Override
  public void onPostReconciliation(StackGresDbOps config) {
    dbOpsScheduler.updateStatus(config,
        StackGresDbOps::getStatus, (targetDbOps, status) -> {
          statusManager.refreshCondition(targetDbOps);
        });
  }

  @Override
  public void onConfigCreated(StackGresDbOps dbOps, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(dbOps, result);
    eventController.sendEvent(DbOpsEventReason.DBOPS_CREATED,
        "DbOps " + dbOps.getMetadata().getNamespace() + "."
            + dbOps.getMetadata().getName() + " created: " + resourceChanged, dbOps);
  }

  @Override
  public void onConfigUpdated(StackGresDbOps dbOps, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(dbOps, result);
    eventController.sendEvent(DbOpsEventReason.DBOPS_UPDATED,
        "DbOps " + dbOps.getMetadata().getNamespace() + "."
            + dbOps.getMetadata().getName() + " updated: " + resourceChanged, dbOps);
  }

  @Override
  public void onError(Exception ex, StackGresDbOps dbOps) {
    String message = MessageFormatter.arrayFormat(
        "DbOps reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(DbOpsEventReason.DBOPS_CONFIG_ERROR,
        message + ": " + ex.getMessage(), dbOps);
  }

  @Inject
  public void setEventController(EventEmitter<StackGresDbOps> eventController) {
    this.eventController = eventController;
  }

  @Inject
  public void setResourceComparator(ComparisonDelegator<StackGresDbOps> resourceComparator) {
    this.patchResumer = new PatchResumer<>(resourceComparator);
  }

  @Inject
  public void setStatusManager(
      StatusManager<StackGresDbOps, StackGresDbOpsCondition> statusManager) {
    this.statusManager = statusManager;
  }

  @Inject
  public void setDbOpsScheduler(CustomResourceScheduler<StackGresDbOps> dbOpsScheduler) {
    this.dbOpsScheduler = dbOpsScheduler;
  }

}
