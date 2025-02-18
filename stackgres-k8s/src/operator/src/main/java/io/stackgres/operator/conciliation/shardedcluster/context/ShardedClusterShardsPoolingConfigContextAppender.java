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
public class ShardedClusterShardsPoolingConfigContextAppender
    extends ContextAppender<StackGresShardedCluster, Builder> {

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;
  private final DefaultPoolingConfigFactory defaultPoolingConfigFactory;

  public ShardedClusterShardsPoolingConfigContextAppender(
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      DefaultPoolingConfigFactory defaultPoolingConfigFactory) {
    this.poolingConfigFinder = poolingConfigFinder;
    this.defaultPoolingConfigFactory = defaultPoolingConfigFactory;
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    final Optional<StackGresPoolingConfig> shardsPoolingConfig = Optional
        .ofNullable(cluster.getSpec().getShards().getConfigurations().getSgPoolingConfig())
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(
                poolingConfigName,
                cluster.getMetadata().getNamespace()));
    if (!cluster.getSpec().getShards().getConfigurations().getSgPoolingConfig()
        .equals(defaultPoolingConfigFactory.getDefaultResourceName(cluster))
        && !Optional.ofNullable(cluster.getSpec().getShards()
            .getPods().getDisableConnectionPooling()).orElse(false)
        && shardsPoolingConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPoolingConfig.KIND + " "
              + cluster.getSpec().getShards().getConfigurations().getSgPoolingConfig()
              + " was not found");
    }
    contextBuilder.shardsPoolingConfig(shardsPoolingConfig);
  }

}
