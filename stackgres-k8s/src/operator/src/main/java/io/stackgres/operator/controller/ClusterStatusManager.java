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

import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCondition;
import io.stackgres.common.crd.sgcluster.StackGresClusterDefinition;
import io.stackgres.common.crd.sgcluster.StackGresClusterDoneable;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;

@ApplicationScoped
public class ClusterStatusManager {

  @Inject
  KubernetesClientFactory clientFactory;

  /**
   * Send an event related to a stackgres cluster.
   */
  public void sendCondition(@NotNull ClusterStatusCondition reason,
      @NotNull StackGresClusterContext context) {
    try (KubernetesClient client = clientFactory.create()) {
      sendCondition(reason, context, client);
    }
  }

  private void sendCondition(ClusterStatusCondition reason, StackGresClusterContext context,
      KubernetesClient client) {
    final StackGresCluster cluster = context.getCluster();
    final Instant now = Instant.now();
    final StackGresClusterCondition condition = reason.getCondition();
    condition.setLastTransitionTime(now.toString());

    if (cluster.getStatus() == null) {
      cluster.setStatus(new StackGresClusterStatus());
    }

    if (cluster.getStatus().getConditions().stream().anyMatch(
        c -> Instant.parse(c.getLastTransitionTime())
            .isBefore(now.plus(1, ChronoUnit.MINUTES)))) {
      return;
    }

    // copy list of current conditions
    List<StackGresClusterCondition> copyList = cluster.getStatus().getConditions().stream()
        .filter(c -> !condition.getType().equals(c.getType()))
        .collect(Collectors.toList());

    copyList.add(condition);

    cluster.getStatus().setConditions(copyList);

    ResourceUtil.getCustomResource(client, StackGresClusterDefinition.NAME)
        .map(crd -> client.customResources(crd,
          StackGresCluster.class,
          StackGresClusterList.class,
          StackGresClusterDoneable.class)
          .inNamespace(cluster.getMetadata().getNamespace())
          .withName(cluster.getMetadata().getName())
          .patch(cluster))
        .orElseThrow(() -> new IllegalStateException("StackGres is not correctly installed:"
            + " CRD " + StackGresClusterDefinition.NAME + " not found."));
  }

  /**
   * Update pending restart status condition.
   */
  public void updatePendingRestart(StackGresClusterContext context) {
    try (KubernetesClient client = clientFactory.create()) {
      updatePendingRestart(context, client);
    }
  }

  private void updatePendingRestart(StackGresClusterContext context, KubernetesClient client) {
    if (isPendingRestart(context, client)) {
      sendCondition(ClusterStatusCondition.PATRONI_REQUIRES_RESTART, context, client);
    } else {
      sendCondition(ClusterStatusCondition.FALSE_PENDING_RESTART, context, client);
    }
  }

  /**
   * Check pending restart status condition.
   */
  public boolean isPendingRestart(StackGresClusterContext context) {
    try (KubernetesClient client = clientFactory.create()) {
      return isPendingRestart(context, client);
    }
  }

  private boolean isPendingRestart(StackGresClusterContext context, KubernetesClient client) {
    final String namespace = context.getCluster().getMetadata().getNamespace();
    final Map<String, String> labels = context.patroniClusterLabels();

    PodList pods = client.pods().inNamespace(namespace).withLabels(labels).list();

    return pods.getItems().stream()
        .map(m -> m.getMetadata()).filter(Objects::nonNull)
        .map(a -> a.getAnnotations()).filter(Objects::nonNull)
        .map(e -> e.entrySet()).filter(Objects::nonNull)
        .anyMatch(p -> p.stream()
            .map(v -> v.getValue()).filter(Objects::nonNull)
            .anyMatch(r -> r.contains("pending_restart")));
  }

}
