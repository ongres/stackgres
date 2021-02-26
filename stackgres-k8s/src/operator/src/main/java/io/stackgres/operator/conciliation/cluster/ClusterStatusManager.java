/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCondition;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterStatusManager
    extends ConditionUpdater<StackGresCluster, StackGresClusterCondition>
    implements StatusManager<StackGresCluster, StackGresClusterCondition> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatusManager.class);

  private final LabelFactory<StackGresCluster> labelFactory;

  private final KubernetesClientFactory clientFactory;

  @Inject
  public ClusterStatusManager(LabelFactory<StackGresCluster> labelFactory,
                              KubernetesClientFactory clientFactory) {
    this.labelFactory = labelFactory;
    this.clientFactory = clientFactory;
  }

  private static String getClusterId(StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace() + "/" + cluster.getMetadata().getName();
  }

  @Override
  public void refreshCondition(StackGresCluster source) {
    if (isPendingRestart(source)) {
      withNewClient(client -> updateCondition(getPodRequiresRestart(), source));
    } else {
      withNewClient(client -> updateCondition(getFalsePendingRestart(), source));
    }

  }

  private void withNewClient(Consumer<KubernetesClient> func) {
    try (var client = clientFactory.create()) {
      func.accept(client);
    }
  }

  @Override
  public void updateCondition(StackGresClusterCondition condition, StackGresCluster context) {
    withNewClient(client -> updateCondition(condition, context));
  }

  /**
   * Check pending restart status condition.
   */
  public boolean isPendingRestart(StackGresCluster source) {
    return isStatefulSetPendingRestart(source)
        || isPatroniPendingRestart(source);
  }

  private boolean isStatefulSetPendingRestart(StackGresCluster source) {

    final Boolean statefulSetPendingRestart = getClusterStatefulSet(source)
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

    if (statefulSetPendingRestart) {
      LOGGER.debug("Cluster {} requires restart due to pod template changes", getClusterId(source));
    }
    return statefulSetPendingRestart;

  }

  private boolean isPatroniPendingRestart(StackGresCluster cluster) {
    final Boolean patroniPendingRestart = getClusterStatefulSet(cluster)
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

    if (patroniPendingRestart) {
      LOGGER.debug("Cluster {} requires restart due to patroni's indication ",
          getClusterId(cluster));
    }
    return patroniPendingRestart;
  }

  private Optional<StatefulSet> getClusterStatefulSet(StackGresCluster cluster) {
    try (KubernetesClient client = clientFactory.create()) {
      return client.apps().statefulSets().inNamespace(cluster.getMetadata().getNamespace())
          .withLabels(labelFactory.genericClusterLabels(cluster))
          .list()
          .getItems().stream()
          .filter(sts -> sts.getMetadata().getOwnerReferences()
              .stream().anyMatch(ownerReference -> ownerReference.getKind()
                  .equals(StackGresCluster.KIND)
                  && ownerReference.getName().equals(cluster.getMetadata().getName())))
          .findFirst();

    }
  }

  private List<Pod> getStsPods(StatefulSet sts, StackGresCluster cluster) {
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
  protected List<StackGresClusterCondition> getConditions(
      StackGresCluster source) {
    return Optional.ofNullable(source.getStatus())
        .map(StackGresClusterStatus::getConditions)
        .orElseGet(ArrayList::new);
  }

  @Override
  protected void setConditions(StackGresCluster source,
                               List<StackGresClusterCondition> conditions) {
    if (source.getStatus() == null) {
      source.setStatus(new StackGresClusterStatus());
    }
    source.getStatus().setConditions(conditions);
  }

  protected StackGresClusterCondition getFalsePendingRestart() {
    return ClusterStatusCondition.FALSE_PENDING_RESTART.getCondition();
  }

  protected StackGresClusterCondition getPodRequiresRestart() {
    return ClusterStatusCondition.POD_REQUIRES_RESTART.getCondition();
  }
}
