/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsStatusCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.resource.ConditionUpdater;

@ApplicationScoped
public class DistributedLogsStatusManager
    extends ConditionUpdater<StackGresDistributedLogs, StackGresDistributedLogsCondition>
    implements StatusManager<StackGresDistributedLogs, StackGresDistributedLogsCondition> {

  private final KubernetesClientFactory clientFactory;
  private final LabelFactory<StackGresDistributedLogs> labelFactory;

  @Inject
  public DistributedLogsStatusManager(KubernetesClientFactory clientFactory,
                                      LabelFactory<StackGresDistributedLogs> labelFactory) {
    this.clientFactory = clientFactory;
    this.labelFactory = labelFactory;
  }

  @Override
  public StackGresDistributedLogs refreshCondition(StackGresDistributedLogs source) {
    if (isPendingRestart(source)) {
      updateCondition(getPodRequiresRestart(), source);
    } else {
      updateCondition(getFalsePendingRestart(), source);
    }
    return source;
  }

  /**
   * Check pending restart status condition.
   */
  public boolean isPendingRestart(StackGresDistributedLogs source) {
    return isClusterPendingUpgrade(source)
        || isStatefulSetPendingRestart(source)
        || isPatroniPendingRestart(source);
  }

  private boolean isClusterPendingUpgrade(StackGresDistributedLogs context) {
    final Map<String, String> podClusterLabels =
        labelFactory.patroniClusterLabels(context);

    return getClusterStatefulSet(context)
        .map(sts -> getStsPods(sts, context)
            .stream()
            .filter(pod -> pod.getMetadata() != null
                && pod.getMetadata().getAnnotations() != null
                && podClusterLabels.entrySet().stream()
                .allMatch(podClusterLabel -> pod.getMetadata().getLabels().entrySet().stream()
                    .anyMatch(podLabel -> Objects.equals(podLabel, podClusterLabel))))
            .anyMatch(pod -> Optional.ofNullable(pod.getMetadata().getAnnotations())
                .orElse(ImmutableMap.of())
                .entrySet()
                .stream()
                .noneMatch(e -> e.getKey().equals(StackGresContext.VERSION_KEY)
                    && e.getValue().equals(StackGresProperty.OPERATOR_VERSION.getString()))))
        .orElse(false);

  }

  private boolean isStatefulSetPendingRestart(StackGresDistributedLogs source) {

    return getClusterStatefulSet(source)
        .filter(sts -> Optional.ofNullable(sts.getStatus())
            .map(StatefulSetStatus::getUpdateRevision).isPresent())
        .map(sts -> {
          String statefulSetUpdateRevision = sts.getStatus().getUpdateRevision();

          List<Pod> pods = getStsPods(sts, source);

          return pods.stream()
              .map(pod -> pod.getMetadata().getLabels().get("controller-revision-hash"))
              .anyMatch(controllerRevisionHash ->
                  !Objects.equals(statefulSetUpdateRevision, controllerRevisionHash));
        })
        .orElse(false);

  }

  private boolean isPatroniPendingRestart(StackGresDistributedLogs cluster) {
    return getClusterStatefulSet(cluster)
        .map(sts -> getStsPods(sts, cluster))
        .map(pods -> pods.stream()
            .map(Pod::getMetadata).filter(Objects::nonNull)
            .map(ObjectMeta::getAnnotations).filter(Objects::nonNull)
            .map(Map::entrySet)
            .anyMatch(p -> p.stream()
                .map(Map.Entry::getValue).filter(Objects::nonNull)
                .anyMatch(r -> r.contains("\"pending_restart\":true")))
        )
        .orElse(false);
  }

  private Optional<StatefulSet> getClusterStatefulSet(StackGresDistributedLogs cluster) {
    try (KubernetesClient client = clientFactory.create()) {
      return client.apps().statefulSets().inNamespace(cluster.getMetadata().getNamespace())
          .withLabels(labelFactory.genericClusterLabels(cluster))
          .list()
          .getItems().stream()
          .filter(sts -> sts.getMetadata().getOwnerReferences()
              .stream().anyMatch(ownerReference -> ownerReference.getKind()
                  .equals(StackGresDistributedLogs.KIND)
                  && ownerReference.getName().equals(cluster.getMetadata().getName())))
          .findFirst();

    }
  }

  private List<Pod> getStsPods(StatefulSet sts, StackGresDistributedLogs cluster) {
    final Map<String, String> podClusterLabels =
        labelFactory.patroniClusterLabels(cluster);

    try (KubernetesClient client = clientFactory.create()) {
      return client.pods().inNamespace(sts.getMetadata().getNamespace())
          .withLabels(podClusterLabels)
          .list()
          .getItems().stream()
          .filter(pod -> pod.getMetadata().getOwnerReferences().stream()
              .anyMatch(ownerReference -> ownerReference.getKind().equals("StatefulSet")
                  && ownerReference.getName().equals(sts.getMetadata().getName())))
          .collect(Collectors.toUnmodifiableList());
    }
  }

  @Override
  protected List<StackGresDistributedLogsCondition> getConditions(
      StackGresDistributedLogs distributedLogs) {
    return Optional.ofNullable(distributedLogs.getStatus())
        .map(StackGresDistributedLogsStatus::getConditions)
        .orElseGet(ArrayList::new);
  }

  @Override
  protected void setConditions(StackGresDistributedLogs distributedLogs,
                               List<StackGresDistributedLogsCondition> conditions) {
    if (distributedLogs.getStatus() == null) {
      distributedLogs.setStatus(new StackGresDistributedLogsStatus());
    }
    distributedLogs.getStatus().setConditions(conditions);
  }

  protected StackGresDistributedLogsCondition getFalsePendingRestart() {
    return DistributedLogsStatusCondition.FALSE_PENDING_RESTART.getCondition();
  }

  protected StackGresDistributedLogsCondition getPodRequiresRestart() {
    return DistributedLogsStatusCondition.POD_REQUIRES_RESTART.getCondition();
  }
}
