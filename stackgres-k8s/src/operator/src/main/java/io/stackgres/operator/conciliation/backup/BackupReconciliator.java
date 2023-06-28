/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgbackup.BackupEventReason;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
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
public class BackupReconciliator
    extends AbstractReconciliator<StackGresBackup> {

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresBackup> scanner;
    @Inject CustomResourceFinder<StackGresBackup> finder;
    @Inject Conciliator<StackGresBackup> conciliator;
    @Inject HandlerDelegator<StackGresBackup> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject EventEmitter<StackGresBackup> eventController;
    @Inject ComparisonDelegator<StackGresBackup> resourceComparator;
    @Inject CustomResourceScheduler<StackGresBackup> backupScheduler;
    @Inject BackupStatusManager statusManager;
  }

  private final EventEmitter<StackGresBackup> eventController;
  private final PatchResumer<StackGresBackup> patchResumer;
  private final CustomResourceScheduler<StackGresBackup> backupScheduler;
  private final BackupStatusManager statusManager;

  @Inject
  public BackupReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.handlerDelegator,
        parameters.client, StackGresBackup.KIND);
    this.eventController = parameters.eventController;
    this.patchResumer = new PatchResumer<>(parameters.resourceComparator);
    this.backupScheduler = parameters.backupScheduler;
    this.statusManager = parameters.statusManager;
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresBackup configKey, boolean load) {
    super.reconciliationCycle(configKey, load);
  }

  @Override
  protected void onPreReconciliation(StackGresBackup config) {
    backupScheduler.update(config, statusManager::refreshCondition);
  }

  @Override
  protected void onPostReconciliation(StackGresBackup config) {
  }

  @Override
  protected void onConfigCreated(StackGresBackup backup, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(backup, result);
    eventController.sendEvent(BackupEventReason.BACKUP_CREATED,
        "Backup " + backup.getMetadata().getNamespace() + "."
            + backup.getMetadata().getName() + " created: " + resourceChanged, backup);
  }

  @Override
  protected void onConfigUpdated(StackGresBackup backup, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(backup, result);
    eventController.sendEvent(BackupEventReason.BACKUP_UPDATED,
        "Backup " + backup.getMetadata().getNamespace() + "."
            + backup.getMetadata().getName() + " updated: " + resourceChanged, backup);
  }

  @Override
  protected void onError(Exception ex, StackGresBackup backup) {
    String message = MessageFormatter.arrayFormat(
        "Backup reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(BackupEventReason.BACKUP_CONFIG_ERROR,
        message + ": " + ex.getMessage(), backup);
  }

}
