/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBindingStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgshardedcluster.ShardedClusterStatusCondition;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operator.conciliation.factory.shardedcluster.ServiceBindingSecret;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShardedClusterStatusManager
    extends ConditionUpdater<StackGresShardedCluster, Condition>
    implements StatusManager<StackGresShardedCluster, Condition> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShardedClusterStatusManager.class);

  private final LabelFactoryForShardedCluster labelFactory;

  private final KubernetesClient client;

  @Inject
  public ShardedClusterStatusManager(LabelFactoryForShardedCluster labelFactory,
      KubernetesClient client) {
    this.labelFactory = labelFactory;
    this.client = client;
  }

  private static String getClusterId(StackGresShardedCluster cluster) {
    return cluster.getMetadata().getNamespace() + "/" + cluster.getMetadata().getName();
  }

  @Override
  public StackGresShardedCluster refreshCondition(StackGresShardedCluster source) {
    if (source.getStatus() == null) {
      source.setStatus(new StackGresShardedClusterStatus());
    }
    source.getStatus().setBinding(new StackGresClusterServiceBindingStatus());
    source.getStatus().getBinding().setName(ServiceBindingSecret.name(source));
    List<StackGresCluster> clusters = getClusters(source);
    if (isPendingRestart(clusters)) {
      updateCondition(getShardedClusterRequiresRestart(), source);
    } else {
      updateCondition(getFalsePendingRestart(), source);
    }
    refreshPendingUpgrade(source, clusters);
    if (isBootstrapped(source, clusters)) {
      updateCondition(getShardedClusterBootstrapped(), source);
    }
    return source;
  }

  /**
   * Check pending restart status condition.
   */
  public boolean isPendingRestart(List<StackGresCluster> clusters) {
    return clusters.stream()
        .flatMap(cluster -> Optional.of(cluster)
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getConditions)
            .stream()
            .flatMap(List::stream))
        .anyMatch(ClusterStatusCondition.POD_REQUIRES_RESTART::isCondition);
  }

  /**
   * Refresh the pending upgrade status condition.
   *
   * <p>The condition is aggregated from the SGShardedCluster itself and from its SGClusters, the
   * same way the pending restart condition is, so that a sharded cluster whose children still
   * require an upgrade does not report PendingUpgrade=False. Unlike the ComponentsUpdated
   * condition of a SGCluster, which is about Postgres and extension versions, this condition is
   * only about the version of the operator that created the resources, and is cleared by a
   * SGShardedDbOps of op securityUpgrade.</p>
   */
  private void refreshPendingUpgrade(
      StackGresShardedCluster shardedCluster,
      List<StackGresCluster> clusters) {
    final String operatorVersion = StackGresProperty.OPERATOR_VERSION.getString();
    final boolean shardedClusterRequiresUpgrade = isPendingUpgrade(shardedCluster);
    final List<String> clustersRequiringUpgrade = clusters.stream()
        .filter(this::isPendingUpgrade)
        .map(cluster -> cluster.getMetadata().getName())
        .toList();
    if (!shardedClusterRequiresUpgrade && clustersRequiringUpgrade.isEmpty()) {
      updateCondition(getFalsePendingUpgrade(), shardedCluster);
      return;
    }
    LOGGER.debug("Sharded Cluster {} requires upgrade since it is using an old operator version",
        getClusterId(shardedCluster));
    final StringBuilder message = new StringBuilder();
    if (shardedClusterRequiresUpgrade) {
      message.append("This SGShardedCluster was created with operator version ")
          .append(getOperatorVersion(shardedCluster))
          .append(" while the running operator version is ")
          .append(operatorVersion)
          .append(".");
    }
    if (!clustersRequiringUpgrade.isEmpty()) {
      if (message.length() > 0) {
        message.append(" ");
      }
      message.append("The SGCluster")
          .append(clustersRequiringUpgrade.size() > 1 ? "s " : " ")
          .append(String.join(", ", clustersRequiringUpgrade))
          .append(clustersRequiringUpgrade.size() > 1 ? " were" : " was")
          .append(" created with an operator version older than ")
          .append(operatorVersion)
          .append(".");
    }
    message.append(" Create a SGShardedDbOps of op securityUpgrade to complete the upgrade.");
    Condition condition = getShardedClusterRequiresUpgrade();
    condition.setMessage(message.toString());
    updateCondition(condition, shardedCluster);
  }

  /**
   * Check pending upgrade status condition of the SGShardedCluster.
   */
  private boolean isPendingUpgrade(StackGresShardedCluster shardedCluster) {
    return StackGresVersion.getStackGresVersion(shardedCluster) != StackGresVersion.LATEST;
  }

  /**
   * Check pending upgrade status condition of a SGCluster.
   */
  private boolean isPendingUpgrade(StackGresCluster cluster) {
    return StackGresVersion.getStackGresVersion(cluster) != StackGresVersion.LATEST;
  }

  private String getOperatorVersion(StackGresShardedCluster shardedCluster) {
    return Optional.ofNullable(shardedCluster.getMetadata().getAnnotations())
        .map(annotations -> annotations.get(StackGresContext.VERSION_KEY))
        .orElse("<unknown>");
  }

  /**
   * Check bootstrapped status condition.
   */
  public boolean isBootstrapped(
      StackGresShardedCluster source,
      List<StackGresCluster> clusters) {
    if (Optional.ofNullable(source.getStatus())
        .map(StackGresShardedClusterStatus::getSgBackups)
        .map(List::size)
        .map(size -> size > clusters.size())
        .orElse(false)) {
      return false;
    } else  if (source.getSpec().getWorkers().getClusters() + 1 != clusters.size()) {
      return false;
    }
    return clusters.stream()
        .flatMap(cluster -> Optional.of(cluster)
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getConditions)
            .stream()
            .flatMap(List::stream))
        .allMatch(ClusterStatusCondition.CLUSTER_BOOTSTRAPPED::isCondition);
  }

  private List<StackGresCluster> getClusters(StackGresShardedCluster shardedCluster) {
    final Map<String, String> clusterLabels =
        labelFactory.genericLabels(shardedCluster);

    return client.resources(StackGresCluster.class)
        .inNamespace(shardedCluster.getMetadata().getNamespace())
        .withLabels(clusterLabels)
        .list()
        .getItems().stream()
        .filter(cluster -> cluster.getMetadata().getOwnerReferences().stream()
            .anyMatch(
                ownerReference -> ownerReference.getKind().equals(StackGresShardedCluster.KIND)
                && ownerReference.getName().equals(shardedCluster.getMetadata().getName())))
        .toList();
  }

  @Override
  protected List<Condition> getConditions(
      StackGresShardedCluster source) {
    return Optional.ofNullable(source.getStatus())
        .map(StackGresShardedClusterStatus::getConditions)
        .orElse(List.of());
  }

  @Override
  protected void setConditions(
      StackGresShardedCluster source,
      List<Condition> conditions) {
    if (source.getStatus() == null) {
      source.setStatus(new StackGresShardedClusterStatus());
    }
    source.getStatus().setConditions(conditions);
  }

  protected Condition getFalsePendingRestart() {
    return ShardedClusterStatusCondition.FALSE_PENDING_RESTART.getCondition();
  }

  protected Condition getShardedClusterRequiresRestart() {
    return ShardedClusterStatusCondition.SHARDED_CLUSTER_REQUIRES_RESTART.getCondition();
  }

  protected Condition getFalsePendingUpgrade() {
    return ShardedClusterStatusCondition.FALSE_PENDING_UPGRADE.getCondition();
  }

  protected Condition getShardedClusterRequiresUpgrade() {
    return ShardedClusterStatusCondition.SHARDED_CLUSTER_REQUIRES_UPGRADE.getCondition();
  }

  protected Condition getShardedClusterBootstrapped() {
    return ShardedClusterStatusCondition.SHARDED_CLUSTER_BOOTSTRAPPED.getCondition();
  }
}
