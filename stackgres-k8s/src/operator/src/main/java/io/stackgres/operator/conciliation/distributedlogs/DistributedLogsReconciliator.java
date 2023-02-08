/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsEventReason;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsStatusCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusCluster;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class DistributedLogsReconciliator extends AbstractReconciliator<StackGresDistributedLogs> {

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresDistributedLogs> scanner;
    @Inject Conciliator<StackGresDistributedLogs> conciliator;
    @Inject HandlerDelegator<StackGresDistributedLogs> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject ConnectedClustersScanner connectedClustersScanner;
    @Inject CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
    @Inject StatusManager<StackGresDistributedLogs,
        StackGresDistributedLogsCondition> statusManager;
    @Inject EventEmitter<StackGresDistributedLogs> eventController;
  }

  private final ConnectedClustersScanner connectedClustersScanner;
  private final CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;
  private final StatusManager<StackGresDistributedLogs,
      StackGresDistributedLogsCondition> statusManager;
  private final EventEmitter<StackGresDistributedLogs> eventController;

  @Inject
  public DistributedLogsReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.conciliator, parameters.handlerDelegator,
        parameters.client, StackGresDistributedLogs.KIND);
    this.connectedClustersScanner = parameters.connectedClustersScanner;
    this.distributedLogsScheduler = parameters.distributedLogsScheduler;
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  public void onPreReconciliation(StackGresDistributedLogs config) {
  }

  @Override
  public void onPostReconciliation(StackGresDistributedLogs config) {
    refreshConnectedClusters(config);

    statusManager.refreshCondition(config);
    distributedLogsScheduler.update(config,
        (targetDistributedLogs, distributedLogsWithStatus) -> {
          var targetPodStatuses = Optional.ofNullable(targetDistributedLogs.getStatus())
              .map(StackGresDistributedLogsStatus::getPodStatuses)
              .orElse(null);
          if (distributedLogsWithStatus.getStatus() != null) {
            distributedLogsWithStatus.getStatus().setPodStatuses(targetPodStatuses);
          }
          targetDistributedLogs.setStatus(distributedLogsWithStatus.getStatus());
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

}
