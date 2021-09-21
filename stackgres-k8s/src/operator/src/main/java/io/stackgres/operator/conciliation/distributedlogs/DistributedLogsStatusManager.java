/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ClusterPendingRestartUtil;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReason;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgdistributedlogs.DistributedLogsStatusCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsCondition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DistributedLogsStatusManager
    extends ConditionUpdater<StackGresDistributedLogs, StackGresDistributedLogsCondition>
    implements StatusManager<StackGresDistributedLogs, StackGresDistributedLogsCondition> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLogsStatusManager.class);

  private final KubernetesClient client;
  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  @Inject
  public DistributedLogsStatusManager(KubernetesClient client,
      LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    this.client = client;
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

  private static String getDistributedLogsId(StackGresDistributedLogs distributedLogs) {
    return distributedLogs.getMetadata().getNamespace() + "/"
        + distributedLogs.getMetadata().getName();
  }

  /**
   * Check pending restart status condition.
   */
  public boolean isPendingRestart(StackGresDistributedLogs distributedLogs) {
    List<StackGresClusterPodStatus> clusterPodStatuses = Optional.ofNullable(
        distributedLogs.getStatus())
        .map(StackGresDistributedLogsStatus::getPodStatuses)
        .orElse(ImmutableList.of());
    Optional<StatefulSet> clusterStatefulSet = getClusterStatefulSet(distributedLogs);
    List<Pod> clusterPods = clusterStatefulSet.map(sts -> getStsPods(sts, distributedLogs))
        .orElse(ImmutableList.of());
    RestartReasons reasons = ClusterPendingRestartUtil.getRestartReasons(
        clusterPodStatuses, clusterStatefulSet, clusterPods);
    for (RestartReason reason : reasons.getReasons()) {
      switch (reason) {
        case OPERATOR_VERSION:
          LOGGER.debug("Distributed Logs {} requires restart due to operator version change",
              getDistributedLogsId(distributedLogs));
          break;
        case PATRONI:
          LOGGER.debug("Distributed Logs {} requires restart due to patroni's indication",
              getDistributedLogsId(distributedLogs));
          break;
        case POD_STATUS:
          LOGGER.debug("Distributed Logs {} requires restart due to pod status indication",
              getDistributedLogsId(distributedLogs));
          break;
        case STATEFULSET:
          LOGGER.debug("Distributed Logs {} requires restart due to pod template changes",
              getDistributedLogsId(distributedLogs));
          break;
        default:
          break;
      }
    }
    return reasons.requiresRestart();
  }

  private Optional<StatefulSet> getClusterStatefulSet(StackGresDistributedLogs cluster) {
    return client.apps().statefulSets().inNamespace(cluster.getMetadata().getNamespace())
        .withLabels(labelFactory.genericLabels(cluster))
        .list()
        .getItems().stream()
        .filter(sts -> sts.getMetadata().getOwnerReferences()
            .stream().anyMatch(ownerReference -> ownerReference.getKind()
                .equals(StackGresDistributedLogs.KIND)
                && ownerReference.getName().equals(cluster.getMetadata().getName())))
        .findFirst();
  }

  private List<Pod> getStsPods(StatefulSet sts, StackGresDistributedLogs cluster) {
    final Map<String, String> podClusterLabels =
        labelFactory.patroniClusterLabels(cluster);

    return client.pods().inNamespace(sts.getMetadata().getNamespace())
        .withLabels(podClusterLabels)
        .list()
        .getItems().stream()
        .filter(pod -> pod.getMetadata().getOwnerReferences().stream()
            .anyMatch(ownerReference -> ownerReference.getKind().equals("StatefulSet")
                && ownerReference.getName().equals(sts.getMetadata().getName())))
        .collect(Collectors.toUnmodifiableList());
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
