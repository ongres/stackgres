/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.coordination.v1.Lease;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedClusterConciliator extends AbstractConciliator<StackGresShardedCluster> {

  private final LabelFactoryForShardedCluster labelFactory;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public ShardedClusterConciliator(
      KubernetesClient client,
      CustomResourceFinder<StackGresShardedCluster> finder,
      RequiredResourceGenerator<StackGresShardedCluster> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresShardedCluster> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache,
      LabelFactoryForShardedCluster labelFactory,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    super(client, finder, requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
    this.labelFactory = labelFactory;
    this.clusterFinder = clusterFinder;
  }

  @Override
  protected boolean skipDeletion(HasMetadata foundDeployedResource, StackGresShardedCluster config) {
    if (foundDeployedResource instanceof StackGresCluster foundDeployedCluster) {
      if (isMajorVersionUpgradeInProgress(config)) {
        return true;
      }
      if (Optional.of(foundDeployedCluster)
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getInstances)
          .orElse(0) == 0) {
        return true;
      }
      return isRegisteredInCitus(foundDeployedCluster, config);
    }
    // The reconciliation handlers of these resources deliberately never delete them. Reporting
    // them as deletions would make the reconciliation never reach a converged state: the
    // resources would be listed as deleted on every cycle and a ClusterUpdated event would be
    // sent for each of them, forever (see https://gitlab.com/ongresinc/stackgres/-/issues/3219).
    if (foundDeployedResource instanceof Lease) {
      return true;
    }
    if ((foundDeployedResource instanceof StackGresPostgresConfig
        || foundDeployedResource instanceof StackGresInstanceProfile
        || foundDeployedResource instanceof StackGresPoolingConfig)
        && isDefaultConfig(foundDeployedResource, config)) {
      return true;
    }
    return super.skipDeletion(foundDeployedResource, config);
  }

  /**
   * A worker or query router SGCluster removed from a Citus SGShardedCluster is not scaled down to
   * 0 instances while its group is still registered in {@code pg_dist_node}, since Citus can only
   * remove a primary node while it can be reached. Once the node has been removed from
   * {@code pg_dist_node} (see
   * {@code SGShardedCluster.spec.configurations.citus.enableNodeAutoRemoval}) the coordinator
   * SGScript stops reporting its group and the SGCluster is scaled down.
   */
  private boolean isRegisteredInCitus(
      StackGresCluster foundDeployedCluster,
      StackGresShardedCluster config) {
    if (!StackGresShardingType.CITUS.equals(
        StackGresShardingType.fromString(config.getSpec().getType()))) {
      return false;
    }
    final boolean result = StackGresShardedClusterForCitusUtil.getRegisteredClusterNames(
        config,
        clusterFinder.findByNameAndNamespace(
            StackGresShardedClusterUtil.getCoordinatorClusterName(config),
            config.getMetadata().getNamespace()))
        .contains(foundDeployedCluster.getMetadata().getName());
    if (result) {
      LOGGER.debug("Skip deletion of {} {}.{} since its group is registered in pg_dist_node",
          StackGresCluster.KIND,
          foundDeployedCluster.getMetadata().getNamespace(),
          foundDeployedCluster.getMetadata().getName());
    }
    return result;
  }

  private boolean isDefaultConfig(
      HasMetadata foundDeployedResource,
      StackGresShardedCluster config) {
    final Map<String, String> labels = Optional.of(foundDeployedResource.getMetadata())
        .map(ObjectMeta::getLabels)
        .orElse(Map.of());
    return labelFactory.defaultConfigLabels(config)
        .entrySet()
        .stream()
        .allMatch(defaultConfigLabel -> labels.entrySet().stream()
            .anyMatch(defaultConfigLabel::equals));
  }

  @Override
  protected boolean skipUpdate(HasMetadata requiredResource, StackGresShardedCluster config) {
    // While a major version upgrade SGShardedDbOps is in progress the per-component child SGDbOps
    // are in full control of each child SGCluster. Stop reconciling (patching) child SGClusters so
    // that the SGShardedCluster does not revert the changes performed by the child SGDbOps.
    if (requiredResource instanceof StackGresCluster
        && isMajorVersionUpgradeInProgress(config)) {
      return true;
    }
    return super.skipUpdate(requiredResource, config);
  }

  private static boolean isMajorVersionUpgradeInProgress(StackGresShardedCluster config) {
    return Optional.ofNullable(config.getStatus())
        .map(StackGresShardedClusterStatus::getDbOps)
        .map(StackGresShardedClusterDbOpsStatus::getMajorVersionUpgrade)
        .isPresent();
  }

}
