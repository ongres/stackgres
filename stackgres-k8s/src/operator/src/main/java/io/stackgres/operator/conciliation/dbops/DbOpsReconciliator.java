/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgcluster.DbOpsEventReason;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.PatchResumer;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.ComparisonDelegator;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DbOpsReconciliator
    extends AbstractReconciliator<StackGresDbOps> {

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresDbOps> scanner;
    @Inject Conciliator<StackGresDbOps> conciliator;
    @Inject HandlerDelegator<StackGresDbOps> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject EventEmitter<StackGresDbOps> eventController;
    @Inject ComparisonDelegator<StackGresDbOps> resourceComparator;
    @Inject DbOpsStatusManager statusManager;
    @Inject CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;
  }

  private final EventEmitter<StackGresDbOps> eventController;
  private final PatchResumer<StackGresDbOps> patchResumer;
  private final DbOpsStatusManager statusManager;
  private final CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;

  @Inject
  public DbOpsReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.conciliator, parameters.handlerDelegator,
        parameters.client, StackGresDbOps.KIND);
    this.eventController = parameters.eventController;
    this.patchResumer = new PatchResumer<>(parameters.resourceComparator);
    this.statusManager = parameters.statusManager;
    this.dbOpsScheduler = parameters.dbOpsScheduler;
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(List<StackGresDbOps> configs) {
    super.reconciliationCycle(configs);
  }

  @Override
  protected void onPreReconciliation(StackGresDbOps config) {
  }

  @Override
  protected void onPostReconciliation(StackGresDbOps config) {
    dbOpsScheduler.update(config, statusManager::refreshCondition);
  }

  @Override
  protected void onConfigCreated(StackGresDbOps dbOps, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(dbOps, result);
    eventController.sendEvent(DbOpsEventReason.DBOPS_CREATED,
        "DbOps " + dbOps.getMetadata().getNamespace() + "."
            + dbOps.getMetadata().getName() + " created: " + resourceChanged, dbOps);
  }

  @Override
  protected void onConfigUpdated(StackGresDbOps dbOps, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(dbOps, result);
    eventController.sendEvent(DbOpsEventReason.DBOPS_UPDATED,
        "DbOps " + dbOps.getMetadata().getNamespace() + "."
            + dbOps.getMetadata().getName() + " updated: " + resourceChanged, dbOps);
  }

  @Override
  protected void onError(Exception ex, StackGresDbOps dbOps) {
    String message = MessageFormatter.arrayFormat(
        "DbOps reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(DbOpsEventReason.DBOPS_CONFIG_ERROR,
        message + ": " + ex.getMessage(), dbOps);
  }

}
