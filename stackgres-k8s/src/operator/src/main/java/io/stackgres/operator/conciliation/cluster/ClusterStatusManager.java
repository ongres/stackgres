/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ClusterPendingRestartUtil;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReason;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterStatusManager
    extends ConditionUpdater<StackGresCluster, Condition>
    implements StatusManager<StackGresCluster, Condition> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatusManager.class);

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  private final KubernetesClient client;

  @Inject
  public ClusterStatusManager(LabelFactoryForCluster<StackGresCluster> labelFactory,
      KubernetesClient client) {
    this.labelFactory = labelFactory;
    this.client = client;
  }

  private static String getClusterId(StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace() + "/" + cluster.getMetadata().getName();
  }

  @Override
  public StackGresCluster refreshCondition(StackGresCluster source) {
    if (isPendingRestart(source)) {
      updateCondition(getPodRequiresRestart(), source);
    } else {
      updateCondition(getFalsePendingRestart(), source);
    }
    if (isPendingUpgrade(source)) {
      updateCondition(getClusterRequiresUpgrade(), source);
    } else {
      updateCondition(getFalsePendingUpgrade(), source);
    }
    return source;
  }

  /**
   * Check pending restart status condition.
   */
  public boolean isPendingRestart(StackGresCluster cluster) {
    List<StackGresClusterPodStatus> clusterPodStatuses = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .orElse(ImmutableList.of());
    Optional<StatefulSet> clusterStatefulSet = getClusterStatefulSet(cluster);
    List<Pod> clusterPods = clusterStatefulSet.map(sts -> getStsPods(sts, cluster))
        .orElse(ImmutableList.of());
    RestartReasons reasons = ClusterPendingRestartUtil.getRestartReasons(
        clusterPodStatuses, clusterStatefulSet, clusterPods);
    for (RestartReason reason : reasons.getReasons()) {
      switch (reason) {
        case PATRONI:
          LOGGER.debug("Cluster {} requires restart due to patroni's indication",
              getClusterId(cluster));
          break;
        case POD_STATUS:
          LOGGER.debug("Cluster {} requires restart due to pod status indication",
              getClusterId(cluster));
          break;
        case STATEFULSET:
          LOGGER.debug("Cluster {} requires restart due to pod template changes",
              getClusterId(cluster));
          break;
        default:
          break;
      }
    }
    return reasons.requiresRestart();
  }

  /**
   * Check pending upgrade status condition.
   */
  public boolean isPendingUpgrade(StackGresCluster cluster) {
    if (Optional.of(cluster.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .anyMatch(e -> e.getKey().equals(StackGresContext.VERSION_KEY)
            && !e.getValue().equals(StackGresProperty.OPERATOR_VERSION.getString()))) {
      LOGGER.debug("Cluster {} requires upgrade since it is using an old operator version",
          getClusterId(cluster));
      return true;
    }
    return false;
  }

  private Optional<StatefulSet> getClusterStatefulSet(StackGresCluster cluster) {
    return Optional.ofNullable(client.apps().statefulSets()
        .inNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getMetadata().getName())
        .get())
        .stream()
        .filter(sts -> sts.getMetadata().getOwnerReferences()
            .stream().anyMatch(ownerReference -> ownerReference.getKind()
                .equals(StackGresCluster.KIND)
                && ownerReference.getName().equals(cluster.getMetadata().getName())
                && ownerReference.getUid().equals(cluster.getMetadata().getUid())))
        .findFirst();
  }

  private List<Pod> getStsPods(StatefulSet sts, StackGresCluster cluster) {
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
  protected List<Condition> getConditions(
      StackGresCluster source) {
    return Optional.ofNullable(source.getStatus())
        .map(StackGresClusterStatus::getConditions)
        .orElseGet(ArrayList::new);
  }

  @Override
  protected void setConditions(
      StackGresCluster source,
      List<Condition> conditions) {
    if (source.getStatus() == null) {
      source.setStatus(new StackGresClusterStatus());
    }
    source.getStatus().setConditions(conditions);
  }

  protected Condition getFalsePendingRestart() {
    return ClusterStatusCondition.FALSE_PENDING_RESTART.getCondition();
  }

  protected Condition getPodRequiresRestart() {
    return ClusterStatusCondition.POD_REQUIRES_RESTART.getCondition();
  }

  protected Condition getFalsePendingUpgrade() {
    return ClusterStatusCondition.FALSE_PENDING_UPGRADE.getCondition();
  }

  protected Condition getClusterRequiresUpgrade() {
    return ClusterStatusCondition.CLUSTER_REQUIRES_UPGRADE.getCondition();
  }
}
