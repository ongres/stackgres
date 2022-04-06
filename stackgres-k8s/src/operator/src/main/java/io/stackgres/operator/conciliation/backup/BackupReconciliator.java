/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgbackup.BackupEventReason;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.event.EventEmitterType;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.PatchResumer;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.ComparisonDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class BackupReconciliator
    extends AbstractReconciliator<StackGresBackup> {

  public BackupReconciliator() {
    super(StackGresBackup.KIND);
  }

  private EventEmitter<StackGresBackup> eventController;

  private PatchResumer<StackGresBackup> patchResumer;

  private CustomResourceScheduler<StackGresBackup> backupScheduler;

  private CustomResourceFinder<StackGresCluster> clusterFinder;

  private CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  private BackupStatusManager statusManager;

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  public void onPreReconciliation(StackGresBackup config) {
    backupScheduler.update(config,
        (targetBackup, backup) -> {
          statusManager.refreshCondition(targetBackup);
        });

    Optional<StackGresCluster> cluster = getCluster(config);
    if (cluster.isPresent()) {
      if (getBackupConfig(cluster.get()).isEmpty()) {
        eventController.sendEvent(BackupEventReason.BACKUP_CONFIG_ERROR,
            "Missing " + StackGresBackupConfig.KIND + " for cluster "
                + cluster.get().getMetadata().getNamespace() + "."
                + cluster.get().getMetadata().getName() + " ", config);
      }
    } else {
      eventController.sendEvent(BackupEventReason.BACKUP_CONFIG_ERROR,
          "Missing " + StackGresCluster.KIND + " "
              + config.getMetadata().getNamespace() + "."
              + config.getSpec().getSgCluster() + " ", config);
    }
  }

  private Optional<StackGresCluster> getCluster(StackGresBackup backup) {
    return Optional.ofNullable(backup.getSpec())
        .map(StackGresBackupSpec::getSgCluster)
        .flatMap(clusterName -> clusterFinder
            .findByNameAndNamespace(clusterName, backup.getMetadata().getNamespace()));
  }

  private Optional<StackGresBackupConfig> getBackupConfig(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getConfiguration)
        .map(StackGresClusterConfiguration::getBackupConfig)
        .flatMap(backupConfigName -> backupConfigFinder
            .findByNameAndNamespace(backupConfigName, cluster.getMetadata().getNamespace()));
  }

  @Override
  public void onPostReconciliation(StackGresBackup config) {
  }

  @Override
  public void onConfigCreated(StackGresBackup backup, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(backup, result);
    eventController.sendEvent(BackupEventReason.BACKUP_CREATED,
        "Backup " + backup.getMetadata().getNamespace() + "."
            + backup.getMetadata().getName() + " created: " + resourceChanged, backup);
  }

  @Override
  public void onConfigUpdated(StackGresBackup backup, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(backup, result);
    eventController.sendEvent(BackupEventReason.BACKUP_UPDATED,
        "Backup " + backup.getMetadata().getNamespace() + "."
            + backup.getMetadata().getName() + " updated: " + resourceChanged, backup);
  }

  @Override
  public void onError(Exception ex, StackGresBackup backup) {
    String message = MessageFormatter.arrayFormat(
        "Backup reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(BackupEventReason.BACKUP_CONFIG_ERROR,
        message + ": " + ex.getMessage(), backup);
  }

  @Inject
  public void setResourceComparator(ComparisonDelegator<StackGresBackup> resourceComparator) {
    this.patchResumer = new PatchResumer<>(resourceComparator);
  }

  @Inject
  public void setBackupScheduler(CustomResourceScheduler<StackGresBackup> backupScheduler) {
    this.backupScheduler = backupScheduler;
  }

  @Inject
  public void setClusterFinder(CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Inject
  public void setBackupConfigFinder(
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder) {
    this.backupConfigFinder = backupConfigFinder;
  }

  @Inject
  public void setEventController(
      @EventEmitterType(StackGresBackup.class)
      EventEmitter<StackGresBackup> eventController) {
    this.eventController = eventController;
  }

  @Inject
  public void setStatusManager(BackupStatusManager statusManager) {
    this.statusManager = statusManager;
  }

}
