/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDefinition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDoneable;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.operator.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ResourceUtil;

@ApplicationScoped
public class DistributedLogsStatusManager {

  private final KubernetesClientFactory clientFactory;

  private final LabelFactory<StackGresDistributedLogs> labelFactory;

  @Inject
  public DistributedLogsStatusManager(KubernetesClientFactory clientFactory,
                                      LabelFactory<StackGresDistributedLogs> labelFactory) {
    this.clientFactory = clientFactory;
    this.labelFactory = labelFactory;
  }

  /**
   * Send an event related to a stackgres centralized logging.
   */
  public void sendCondition(@NotNull DistributedLogsStatusCondition reason,
                            @NotNull StackGresDistributedLogsContext distributedLogsContext) {
    try (KubernetesClient client = clientFactory.create()) {
      sendCondition(reason, distributedLogsContext, client);
    }
  }

  private void sendCondition(DistributedLogsStatusCondition reason,
                             StackGresDistributedLogsContext distributedLogsContext,
                             KubernetesClient client) {
    StackGresDistributedLogs distributedLogs = distributedLogsContext.getDistributedLogs();
    Instant now = Instant.now();

    StackGresDistributedLogsCondition condition = reason.getCondition();
    condition.setLastTransitionTime(now.toString());

    if (distributedLogs.getStatus() == null) {
      distributedLogs.setStatus(new StackGresDistributedLogsStatus());
    }

    if (distributedLogs.getStatus().getConditions().stream().anyMatch(
        c -> Instant.parse(c.getLastTransitionTime())
            .isBefore(now.plus(1, ChronoUnit.MINUTES)))) {
      return;
    }

    // copy list of current conditions
    List<StackGresDistributedLogsCondition> copyList =
        distributedLogs.getStatus().getConditions().stream()
            .filter(c -> !condition.getType().equals(c.getType()))
            .collect(Collectors.toList());

    copyList.add(condition);

    distributedLogs.getStatus().setConditions(copyList);

    ResourceUtil.getCustomResource(client, StackGresDistributedLogsDefinition.NAME)
        .ifPresent(crd -> client.customResources(crd,
            StackGresDistributedLogs.class,
            StackGresDistributedLogsList.class,
            StackGresDistributedLogsDoneable.class)
            .inNamespace(distributedLogs.getMetadata().getNamespace())
            .withName(distributedLogs.getMetadata().getName())
            .patch(distributedLogs));
  }

  /**
   * Update pending restart status condition.
   */
  public void updatePendingRestart(StackGresDistributedLogsContext distributedLogsContext) {
    try (KubernetesClient client = clientFactory.create()) {
      updatePendingRestart(distributedLogsContext, client);
    }
  }

  private void updatePendingRestart(StackGresDistributedLogsContext distributedLogsContext,
                                    KubernetesClient client) {
    if (isPendingRestart(distributedLogsContext, client)) {
      sendCondition(DistributedLogsStatusCondition.PATRONI_REQUIRES_RESTART,
          distributedLogsContext, client);
    } else {
      sendCondition(DistributedLogsStatusCondition.FALSE_PENDING_RESTART,
          distributedLogsContext, client);
    }
  }

  /**
   * Check pending restart status condition.
   */
  public boolean isPendingRestart(StackGresDistributedLogsContext distributedLogsContext) {
    try (KubernetesClient client = clientFactory.create()) {
      return isPendingRestart(distributedLogsContext, client);
    }
  }

  private boolean isPendingRestart(StackGresDistributedLogsContext distributedLogsContext,
                                   KubernetesClient client) {
    StackGresDistributedLogs distributedLogs = distributedLogsContext.getDistributedLogs();
    final String namespace = distributedLogs.getMetadata().getNamespace();
    final StackGresCluster cluster = distributedLogsContext.getCluster();
    final Map<String, String> labels = labelFactory.patroniClusterLabels(cluster);

    PodList pods = client.pods().inNamespace(namespace).withLabels(labels).list();

    return pods.getItems().stream()
        .map(Pod::getMetadata).filter(Objects::nonNull)
        .map(ObjectMeta::getAnnotations).filter(Objects::nonNull)
        .map(Map::entrySet)
        .anyMatch(p -> p.stream()
            .map(Map.Entry::getValue).filter(Objects::nonNull)
            .anyMatch(r -> r.contains("pending_restart")));
  }

}
