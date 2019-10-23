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
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.common.customresource.sgcluster.StackGresClusterCondition;
import io.stackgres.common.customresource.sgcluster.StackGresClusterDefinition;
import io.stackgres.common.customresource.sgcluster.StackGresClusterDoneable;
import io.stackgres.common.customresource.sgcluster.StackGresClusterList;
import io.stackgres.common.customresource.sgcluster.StackGresClusterStatus;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.app.KubernetesClientFactory;

@ApplicationScoped
public class ClusterStatusManager {

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Send an event related to a stackgres cluster.
   */
  public void sendCondition(@NotNull ClusterStatusCondition reason,
      @NotNull StackGresCluster cluster) {
    try (KubernetesClient client = kubClientFactory.create()) {
      sendCondition(reason, cluster, client);
    }
  }

  private void sendCondition(ClusterStatusCondition reason, StackGresCluster cluster,
      KubernetesClient client) {
    Instant now = Instant.now();

    StackGresClusterCondition condition = reason.getCondition();
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

    Optional<CustomResourceDefinition> crd =
        ResourceUtil.getCustomResource(client, StackGresClusterDefinition.NAME);
    if (crd.isPresent()) {
      client.customResources(crd.get(),
          StackGresCluster.class,
          StackGresClusterList.class,
          StackGresClusterDoneable.class)
          .inNamespace(cluster.getMetadata().getNamespace())
          .withName(cluster.getMetadata().getName())
          .patch(cluster);
    }
  }

  /**
   * Update pending restart status condition.
   */
  public void updatePendingRestart(StackGresCluster cluster) {
    try (KubernetesClient client = kubClientFactory.create()) {
      updatePendingRestart(cluster, client);
    }
  }

  private void updatePendingRestart(StackGresCluster cluster, KubernetesClient client) {
    if (isPendingRestart(cluster, client)) {
      sendCondition(ClusterStatusCondition.PATRONI_REQUIRES_RESTART, cluster, client);
    } else {
      sendCondition(ClusterStatusCondition.FALSE_PENDING_RESTART, cluster, client);
    }
  }

  /**
   * Check pending restart status condition.
   */
  public boolean isPendingRestart(StackGresCluster cluster) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return isPendingRestart(cluster, client);
    }
  }

  private boolean isPendingRestart(StackGresCluster cluster, KubernetesClient client) {
    final String name = cluster.getMetadata().getName();
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = ResourceUtil.defaultLabels(name);

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
