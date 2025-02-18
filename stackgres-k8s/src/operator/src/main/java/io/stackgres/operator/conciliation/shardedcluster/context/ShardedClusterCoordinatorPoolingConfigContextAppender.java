/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.Optional;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import io.stackgres.operator.initialization.DefaultPoolingConfigFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterCoordinatorPoolingConfigContextAppender
    extends ContextAppender<StackGresShardedCluster, Builder> {

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;
  private final DefaultPoolingConfigFactory defaultPoolingConfigFactory;

  public ShardedClusterCoordinatorPoolingConfigContextAppender(
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      DefaultPoolingConfigFactory defaultPoolingConfigFactory) {
    this.poolingConfigFinder = poolingConfigFinder;
    this.defaultPoolingConfigFactory = defaultPoolingConfigFactory;
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    final Optional<StackGresPoolingConfig> coordinatorPoolingConfig = Optional
        .ofNullable(cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig())
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(
                poolingConfigName,
                cluster.getMetadata().getNamespace()));
    if (!cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig()
        .equals(defaultPoolingConfigFactory.getDefaultResourceName(cluster))
        && !Optional.ofNullable(cluster.getSpec().getCoordinator()
            .getPods().getDisableConnectionPooling()).orElse(false)
        && coordinatorPoolingConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPoolingConfig.KIND + " "
              + cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig()
              + " was not found");
    }
    contextBuilder.coordinatorPoolingConfig(coordinatorPoolingConfig);
  }

}
