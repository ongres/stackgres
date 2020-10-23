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

import javax.validation.constraints.NotNull;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterContext;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractClusterStatusManager<T extends StackGresClusterContext,
    C extends Condition> {

  private final KubernetesClientFactory clientFactory;
  private final LabelFactory<?> labelFactory;

  public AbstractClusterStatusManager(KubernetesClientFactory clientFactory,
      LabelFactory<?> labelFactory) {
    this.clientFactory = clientFactory;
    this.labelFactory = labelFactory;
  }

  /**
   * Send an event related to a stackgres cluster.
   */
  public void sendCondition(@NotNull C condition,
      @NotNull T context) {
    try (KubernetesClient client = clientFactory.create()) {
      sendCondition(condition, context, client);
    }
  }

  private void sendCondition(C condition, T context, KubernetesClient client) {
    Instant now = Instant.now();

    condition.setLastTransitionTime(now.toString());

    if (getConditions(context).stream()
        .filter(c -> c.getType().equals(condition.getType())
            && c.getStatus().equals(condition.getStatus()))
        .anyMatch(c -> Instant.parse(c.getLastTransitionTime())
            .isBefore(now.plus(1, ChronoUnit.MINUTES)))) {
      return;
    }

    // copy list of current conditions
    List<C> copyList =
        getConditions(context).stream()
            .filter(c -> !condition.getType().equals(c.getType()))
            .collect(Collectors.toList());

    copyList.add(condition);

    setConditions(context, copyList);

    patchCluster(context, client);
  }

  protected abstract List<C> getConditions(T context);

  protected abstract void setConditions(T context, List<C> conditions);

  protected abstract void patchCluster(T context, KubernetesClient client);

  /**
   * Update pending restart status condition.
   */
  public void updatePendingRestart(T context) {
    try (KubernetesClient client = clientFactory.create()) {
      updatePendingRestart(context, client);
    }
  }

  private void updatePendingRestart(T context, KubernetesClient client) {
    if (isPendingRestart(context)) {
      sendCondition(getPodRequiresRestart(), context, client);
    } else {
      sendCondition(getFalsePendingRestart(), context, client);
    }
  }

  protected abstract C getFalsePendingRestart();

  protected abstract C getPodRequiresRestart();

  /**
   * Check pending restart status condition.
   */
  public boolean isPendingRestart(T context) {
    return isStatefulSetPendingRestart(context)
        || isPatroniPendingRestart(context);
  }

  private boolean isStatefulSetPendingRestart(T context) {
    final Map<String, String> clusterLabels = labelFactory.clusterLabels(context.getCluster());
    final Map<String, String> podClusterLabels =
        labelFactory.patroniClusterLabels(context.getCluster());

    String statefulSetUpdateRevision = context.getExistingResources()
        .stream()
        .map(Tuple2::v1)
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .filter(sts -> sts.getMetadata() != null
            && sts.getMetadata().getLabels() != null
            && clusterLabels.entrySet().stream()
            .allMatch(clusterLabel -> sts.getMetadata().getLabels().entrySet().stream()
                .anyMatch(stsLabel -> Objects.equals(stsLabel, clusterLabel))))
        .map(sts -> Optional.ofNullable(sts.getStatus())
            .map(StatefulSetStatus::getUpdateRevision))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElse(null);

    return context.getExistingResources()
        .stream()
        .map(Tuple2::v1)
        .filter(Pod.class::isInstance)
        .map(Pod.class::cast)
        .filter(pod -> pod.getMetadata() != null
            && pod.getMetadata().getLabels() != null
            && podClusterLabels.entrySet().stream()
            .allMatch(podClusterLabel -> pod.getMetadata().getLabels().entrySet().stream()
                .anyMatch(podLabel -> Objects.equals(podLabel, podClusterLabel))))
        .anyMatch(pod -> !Objects.equals(
            statefulSetUpdateRevision,
            pod.getMetadata().getLabels().get("controller-revision-hash")));
  }

  private boolean isPatroniPendingRestart(T context) {
    final StackGresCluster cluster = context.getCluster();
    final Map<String, String> podClusterLabels = labelFactory.patroniClusterLabels(cluster);

    return context.getExistingResources()
        .stream()
        .map(Tuple2::v1)
        .filter(Pod.class::isInstance)
        .map(Pod.class::cast)
        .filter(pod -> pod.getMetadata() != null
            && pod.getMetadata().getLabels() != null
            && podClusterLabels.entrySet().stream()
            .allMatch(podClusterLabel -> pod.getMetadata().getLabels().entrySet().stream()
                .anyMatch(podLabel -> Objects.equals(podLabel, podClusterLabel))))
        .map(Pod::getMetadata).filter(Objects::nonNull)
        .map(ObjectMeta::getAnnotations).filter(Objects::nonNull)
        .map(Map::entrySet)
        .anyMatch(p -> p.stream()
            .map(Map.Entry::getValue).filter(Objects::nonNull)
            .anyMatch(r -> r.contains("\"pending_restart\":true")));
  }

}
