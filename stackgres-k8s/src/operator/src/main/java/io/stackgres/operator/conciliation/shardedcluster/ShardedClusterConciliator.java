/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedClusterConciliator extends AbstractConciliator<StackGresShardedCluster> {

  private final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ShardedClusterConciliator(
      KubernetesClient client,
      CustomResourceFinder<StackGresShardedCluster> finder,
      RequiredResourceGenerator<StackGresShardedCluster> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresShardedCluster> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache,
      LabelFactoryForShardedCluster labelFactory) {
    super(client, finder, requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
    this.labelFactory = labelFactory;
  }

  @Override
  protected boolean skipDeletion(HasMetadata foundDeployedResource, StackGresShardedCluster config) {
    if (foundDeployedResource instanceof StackGresCluster foundDeployedCluster) {
      return Optional.of(foundDeployedCluster)
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getInstances)
          .orElse(0) == 0;
    }
    // The reconciliation handlers of these resources deliberately never delete them. Reporting
    // them as deletions would make the reconciliation never reach a converged state: the
    // resources would be listed as deleted on every cycle and a ClusterUpdated event would be
    // sent for each of them, forever (see https://gitlab.com/ongresinc/stackgres/-/issues/3219).
    if ((foundDeployedResource instanceof StackGresPostgresConfig
        || foundDeployedResource instanceof StackGresProfile
        || foundDeployedResource instanceof StackGresPoolingConfig)
        && isDefaultConfig(foundDeployedResource, config)) {
      return true;
    }
    return super.skipDeletion(foundDeployedResource, config);
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

}
