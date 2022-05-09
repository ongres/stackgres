/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsEventReason;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsStatusCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusCluster;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.event.EventEmitterType;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DistributedLogsReconciliator extends AbstractReconciliator<StackGresDistributedLogs> {

  private static final long VERSION_1_1 = StackGresVersion.V_1_1.getVersionAsNumber();

  public DistributedLogsReconciliator() {
    super(StackGresDistributedLogs.KIND);
  }

  private ConnectedClustersScanner connectedClustersScanner;

  private CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;

  private StatusManager<StackGresDistributedLogs, StackGresDistributedLogsCondition> statusManager;

  private EventEmitter<StackGresDistributedLogs> eventController;

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  public void onPreReconciliation(StackGresDistributedLogs config) {
    if (Optional.of(config)
        .map(StackGresDistributedLogs::getStatus)
        .map(StackGresDistributedLogsStatus::getLabelPrefix)
        .isEmpty()) {
      final long version = StackGresVersion.getStackGresVersionAsNumber(config);
      if (version <= VERSION_1_1) {
        if (config.getStatus() == null) {
          config.setStatus(new StackGresDistributedLogsStatus());
        }
        config.getStatus().setLabelPrefix("");
      }
    }
  }

  @Override
  public void onPostReconciliation(StackGresDistributedLogs config) {
    refreshConnectedClusters(config);

    statusManager.refreshCondition(config);
    distributedLogsScheduler.updateStatus(config,
        StackGresDistributedLogs::getStatus, (targetDistributedLogs, status) -> {
          var targetPodStatuses = Optional.ofNullable(targetDistributedLogs.getStatus())
              .map(StackGresDistributedLogsStatus::getPodStatuses)
              .orElse(null);
          if (status != null) {
            status.setPodStatuses(targetPodStatuses);
          }
          targetDistributedLogs.setStatus(status);
        });
  }

  private void refreshConnectedClusters(StackGresDistributedLogs config) {
    var clusters = connectedClustersScanner.getConnectedClusters(config);

    config.setStatus(
        Optional.ofNullable(config.getStatus())
            .orElseGet(StackGresDistributedLogsStatus::new));
    config.getStatus()
        .setConnectedClusters(clusters.stream()
            .map(cluster -> {
              StackGresDistributedLogsStatusCluster connectedCluster =
                  new StackGresDistributedLogsStatusCluster();
              connectedCluster.setNamespace(cluster.getMetadata().getNamespace());
              connectedCluster.setName(cluster.getMetadata().getName());
              connectedCluster.setConfig(cluster.getSpec().getDistributedLogs());
              return connectedCluster;
            })
            .collect(Collectors.toList()));
  }

  @Override
  public void onConfigCreated(StackGresDistributedLogs distributedLogs,
                              ReconciliationResult result) {
    final ObjectMeta metadata = distributedLogs.getMetadata();
    eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_CREATED,
        "StackGres Centralized Logging " + metadata.getNamespace() + "."
            + metadata.getName() + " created",
        distributedLogs);

    statusManager.updateCondition(
        DistributedLogsStatusCondition.FALSE_FAILED.getCondition(), distributedLogs);
  }

  @Override
  public void onConfigUpdated(StackGresDistributedLogs distributedLogs,
                              ReconciliationResult result) {
    final ObjectMeta metadata = distributedLogs.getMetadata();
    eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_UPDATED,
        "StackGres Centralized Logging " + metadata.getNamespace() + "."
            + metadata.getName() + " updated",
        distributedLogs);
    statusManager.updateCondition(
        DistributedLogsStatusCondition.FALSE_FAILED.getCondition(), distributedLogs);
  }

  @Override
  public void onError(Exception ex, StackGresDistributedLogs context) {

    String message = MessageFormatter.arrayFormat(
        "StackGres DistributeLogs reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_CONFIG_ERROR,
        message + ": " + ex.getMessage(), context);
  }

  @Inject
  public void setConnectedClustersScanner(ConnectedClustersScanner connectedClustersScanner) {
    this.connectedClustersScanner = connectedClustersScanner;
  }

  @Inject
  public void setDistributedLogsScheduler(
      CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler) {
    this.distributedLogsScheduler = distributedLogsScheduler;
  }

  @Inject
  public void setStatusManager(
      StatusManager<StackGresDistributedLogs, StackGresDistributedLogsCondition> statusManager) {
    this.statusManager = statusManager;
  }

  @Inject
  public void setEventController(
      @EventEmitterType(StackGresDistributedLogs.class)
      EventEmitter<StackGresDistributedLogs> eventController) {
    this.eventController = eventController;
  }
}
