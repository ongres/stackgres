/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
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
    if (isPendingUpgrade(source)) {
      updateCondition(getShardedClusterRequiresUpgrade(), source);
    } else {
      updateCondition(getFalsePendingUpgrade(), source);
    }
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
   * Check pending upgrade status condition.
   */
  private boolean isPendingUpgrade(
      StackGresShardedCluster shardedCluster) {
    if (Optional.of(shardedCluster.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .anyMatch(e -> e.getKey().equals(StackGresContext.VERSION_KEY)
            && !e.getValue().equals(StackGresProperty.OPERATOR_VERSION.getString()))) {
      LOGGER.debug("Sharded Cluster {} requires upgrade since it is using an old operator version",
          getClusterId(shardedCluster));
      return true;
    }
    return false;
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
    } else  if (source.getSpec().getShards().getClusters() + 1 != clusters.size()) {
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
