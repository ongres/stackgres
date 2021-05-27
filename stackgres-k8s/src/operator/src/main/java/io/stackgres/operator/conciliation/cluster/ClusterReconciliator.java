/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgbackup.BackupEventReason;
import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCondition;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.conciliation.StackGresReconciliator;
import io.stackgres.operator.conciliation.StatusManager;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ClusterReconciliator
    extends StackGresReconciliator<StackGresCluster> {

  private StatusManager<StackGresCluster, StackGresClusterCondition> statusManager;

  private EventEmitter<StackGresCluster> eventController;

  private EventEmitter<StackGresBackup> backupEventEmitter;

  private CustomResourceScheduler<StackGresCluster> clusterScheduler;

  private CustomResourceScanner<StackGresBackup> backupScanner;

  private CustomResourceScheduler<StackGresBackup> backupScheduler;

  private CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  @Override
  public void onPreReconciliation(StackGresCluster config) {
    String namespace = config.getMetadata().getNamespace();

    List<StackGresBackup> backups = backupScanner.getResources(namespace);

    backups.stream().filter(backup -> Optional.ofNullable(backup.getStatus())
        .map(StackGresBackupStatus::getProcess)
        .map(StackGresBackupProcess::getStatus).isEmpty())
        .forEach(this::initBackup);

    if (!backups.isEmpty() && getBackupConfig(config).isEmpty()) {
      backups.forEach(backup -> {
        backupEventEmitter.sendEvent(BackupEventReason.BACKUP_CONFIG_ERROR,
            "Missing " + StackGresBackupConfig.KIND + " for cluster "
                + config.getMetadata().getNamespace() + "."
                + config.getMetadata().getName() + " ", backup);
      });
    }

  }

  private void initBackup(StackGresBackup backup) {
    backup.setStatus(new StackGresBackupStatus());
    backup.getStatus().setProcess(new StackGresBackupProcess());
    backup.getStatus().getProcess().setStatus(BackupPhase.PENDING.label());
    backupScheduler.update(backup);
  }

  private Optional<StackGresBackupConfig> getBackupConfig(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getConfiguration)
        .map(StackGresClusterConfiguration::getBackupConfig)
        .flatMap(backupConfigName -> backupConfigFinder
            .findByNameAndNamespace(backupConfigName, cluster.getMetadata().getNamespace()));
  }

  @Override
  public void onPostReconciliation(StackGresCluster config) {
    statusManager.refreshCondition(config);
    clusterScheduler.updateStatus(config);
  }

  @Override
  public void onConfigCreated(StackGresCluster cluster) {
    eventController.sendEvent(ClusterEventReason.CLUSTER_CREATED,
        "StackGres Cluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " created", cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  public void onConfigUpdated(StackGresCluster cluster) {

    eventController.sendEvent(ClusterEventReason.CLUSTER_UPDATED,
        "StackGres Cluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " updated", cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  public void onError(Exception ex, StackGresCluster cluster) {
    String message = MessageFormatter.arrayFormat(
        "StackGres Cluster reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ClusterEventReason.CLUSTER_CONFIG_ERROR,
        message + ": " + ex.getMessage(), cluster);
  }

  @Inject
  public void setStatusManager(
      StatusManager<StackGresCluster, StackGresClusterCondition> statusManager) {
    this.statusManager = statusManager;
  }

  @Inject
  public void setEventController(EventEmitter<StackGresCluster> eventController) {
    this.eventController = eventController;
  }

  @Inject
  public void setClusterScheduler(CustomResourceScheduler<StackGresCluster> clusterScheduler) {
    this.clusterScheduler = clusterScheduler;
  }

  @Inject
  public void setBackupScanner(CustomResourceScanner<StackGresBackup> backupScanner) {
    this.backupScanner = backupScanner;
  }

  @Inject
  public void setBackupScheduler(CustomResourceScheduler<StackGresBackup> backupScheduler) {
    this.backupScheduler = backupScheduler;
  }

  @Inject
  public void setBackupConfigFinder(
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder) {
    this.backupConfigFinder = backupConfigFinder;
  }

  @Inject
  public void setBackupEventEmitter(EventEmitter<StackGresBackup> backupEventEmitter) {
    this.backupEventEmitter = backupEventEmitter;
  }
}
