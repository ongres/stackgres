/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsEventReason;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsStatusCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusCluster;
import io.stackgres.common.resource.DistributedLogsScheduler;
import io.stackgres.operator.common.StackGresDistributedLogsContext;
import io.stackgres.operator.resource.DistributedLogsResourceHandlerSelector;
import io.stackgres.operatorframework.reconciliation.ResourceGeneratorReconciliator;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class DistributedLogsReconciliator
    extends ResourceGeneratorReconciliator<StackGresDistributedLogsContext,
      StackGresDistributedLogs, DistributedLogsResourceHandlerSelector> {

  private final DistributedLogsScheduler distributedLogsScheduler;
  private final DistributedLogsStatusManager statusManager;
  private final EventController eventController;

  @Dependent
  public static class Parameters {
    @Inject ObjectMapper objectMapper;
    @Inject DistributedLogsResourceHandlerSelector handlerSelector;
    @Inject DistributedLogsScheduler distributedLogsScheduler;
    @Inject DistributedLogsStatusManager statusManager;
    @Inject EventController eventController;
  }

  @Inject
  public DistributedLogsReconciliator(Parameters parameters) {
    super("Centralized Logging",
        StackGresDistributedLogsContext::getDistributedLogs,
        parameters.handlerSelector,
        parameters.objectMapper);
    this.distributedLogsScheduler = parameters.distributedLogsScheduler;
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
  }

  public DistributedLogsReconciliator() {
    super(null, c -> null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.distributedLogsScheduler = null;
    this.statusManager = null;
    this.eventController = null;
  }

  public static DistributedLogsReconciliator create(Consumer<Parameters> consumer) {
    Stream<Parameters> parameters = Optional.of(new Parameters()).stream().peek(consumer);
    return new DistributedLogsReconciliator(parameters.findAny().get());
  }

  @Override
  protected void onPreConfigReconcilied(KubernetesClient client,
      StackGresDistributedLogsContext context) {
    statusManager.updatePendingRestart(context);
    boolean isRestartPending = statusManager.isPendingRestart(context);
    context.getRequiredResources().stream()
        .map(Tuple2::v1)
        .forEach(resource -> {
          if (!isRestartPending
              && Optional.ofNullable(resource.getMetadata())
              .map(ObjectMeta::getAnnotations)
              .map(annotations -> annotations
                  .get(StackGresContext.RECONCILIATION_PAUSE_UNTIL_RESTART_KEY))
              .map(Boolean::valueOf)
              .orElse(false)) {
            Map<String, String> annotations = new HashMap<>(
                resource.getMetadata().getAnnotations());
            annotations.put(
                StackGresContext.RECONCILIATION_PAUSE_UNTIL_RESTART_KEY,
                String.valueOf(Boolean.FALSE));
            resource.getMetadata().setAnnotations(annotations);
          }
        });
  }

  @Override
  protected void onConfigCreated(KubernetesClient client, StackGresDistributedLogsContext context) {
    StackGresDistributedLogs distributedLogs = context.getDistributedLogs();
    eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_CREATED,
        "StackGres Centralized Logging " + distributedLogs.getMetadata().getNamespace() + "."
        + distributedLogs.getMetadata().getName() + " created",
        distributedLogs, client);
    statusManager.updateCondition(
        DistributedLogsStatusCondition.FALSE_FAILED.getCondition(), context);
    distributedLogsScheduler.updateStatus(context.getDistributedLogs(),
        StackGresDistributedLogs::getStatus,
        StackGresDistributedLogs::setStatus);
  }

  @Override
  protected void onConfigUpdated(KubernetesClient client, StackGresDistributedLogsContext context) {
    StackGresDistributedLogs distributedLogs = context.getDistributedLogs();
    eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_UPDATED,
        "StackGres Centralized Logging " + distributedLogs.getMetadata().getNamespace() + "."
        + distributedLogs.getMetadata().getName() + " updated",
        distributedLogs, client);
    statusManager.updateCondition(
        DistributedLogsStatusCondition.FALSE_FAILED.getCondition(), context);
    distributedLogsScheduler.updateStatus(context.getDistributedLogs(),
        StackGresDistributedLogs::getStatus,
        StackGresDistributedLogs::setStatus);
  }

  @Override
  protected void onPostConfigReconcilied(KubernetesClient client,
      StackGresDistributedLogsContext context) {
    statusManager.updatePendingRestart(context);
    context.getDistributedLogs().setStatus(
        Optional.ofNullable(context.getDistributedLogs().getStatus())
        .orElseGet(() -> new StackGresDistributedLogsStatus()));
    context.getDistributedLogs().getStatus()
        .setConnectedClusters(context.getConnectedClusters().stream()
            .map(cluster -> {
              StackGresDistributedLogsStatusCluster connectedCluster =
                  new StackGresDistributedLogsStatusCluster();
              connectedCluster.setNamespace(cluster.getMetadata().getNamespace());
              connectedCluster.setName(cluster.getMetadata().getName());
              connectedCluster.setConfig(cluster.getSpec().getDistributedLogs());
              return connectedCluster;
            })
            .collect(Collectors.toList()));
    distributedLogsScheduler.update(context.getDistributedLogs());
  }

}
