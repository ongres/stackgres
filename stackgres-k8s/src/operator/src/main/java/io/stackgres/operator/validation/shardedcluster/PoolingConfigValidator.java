/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShard;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class PoolingConfigValidator implements ShardedClusterValidator {

  private final CustomResourceFinder<StackGresPoolingConfig> configFinder;

  @Inject
  public PoolingConfigValidator(
      CustomResourceFinder<StackGresPoolingConfig> configFinder) {
    this.configFinder = configFinder;
  }

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        String coordinatorPoolingConfig = cluster.getSpec().getCoordinator()
            .getConfiguration().getConnectionPoolingConfig();
        checkIfPoolingConfigExists(review, coordinatorPoolingConfig,
            "Pooling config " + coordinatorPoolingConfig
                + " not found for coordinator");
        String shardsPoolingConfig = cluster.getSpec().getShards()
            .getConfiguration().getConnectionPoolingConfig();
        checkIfPoolingConfigExists(review, shardsPoolingConfig,
            "Pooling config " + shardsPoolingConfig
                + " not found for shards");
        for (var overrideShard : Optional.of(cluster.getSpec().getShards())
            .map(StackGresShardedClusterShards::getOverrides)
            .orElse(List.of())) {
          if (overrideShard.getConfigurationForShards() == null
              || overrideShard.getConfigurationForShards().getConnectionPoolingConfig() == null) {
            continue;
          }
          String overrideShardsPoolingConfig = overrideShard
              .getConfigurationForShards().getConnectionPoolingConfig();
          checkIfPoolingConfigExists(review, overrideShardsPoolingConfig,
              "Pooling config " + overrideShardsPoolingConfig
                  + " not found for shard " + overrideShard.getIndex());
        }
        break;
      }
      case UPDATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        StackGresShardedCluster oldCluster = review.getRequest().getOldObject();
        String coordinatorPoolingConfig = cluster.getSpec().getCoordinator()
            .getConfiguration().getConnectionPoolingConfig();
        String oldCoordinatorPoolingConfig = oldCluster.getSpec().getCoordinator()
            .getConfiguration().getConnectionPoolingConfig();
        if (!coordinatorPoolingConfig.equals(oldCoordinatorPoolingConfig)) {
          checkIfPoolingConfigExists(review, coordinatorPoolingConfig,
              "Cannot update coordinator to pooling config "
                  + coordinatorPoolingConfig + " because it doesn't exists");
        }
        String shardsPoolingConfig = cluster.getSpec().getShards()
            .getConfiguration().getConnectionPoolingConfig();
        String oldShardsPoolingConfig = oldCluster.getSpec().getShards()
            .getConfiguration().getConnectionPoolingConfig();
        if (!shardsPoolingConfig.equals(oldShardsPoolingConfig)) {
          checkIfPoolingConfigExists(review, shardsPoolingConfig,
              "Cannot update shards to pooling config "
                  + shardsPoolingConfig + " because it doesn't exists");
        }
        for (var overrideShard : Optional.of(cluster.getSpec().getShards())
            .map(StackGresShardedClusterShards::getOverrides)
            .orElse(List.of())) {
          if (overrideShard.getConfigurationForShards() == null
              || overrideShard.getConfigurationForShards().getConnectionPoolingConfig() == null) {
            continue;
          }
          String overrideShardsPoolingConfig = overrideShard
              .getConfigurationForShards().getConnectionPoolingConfig();
          String oldOverrideShardsPoolingConfig = Optional.of(oldCluster.getSpec().getShards())
              .map(StackGresShardedClusterShards::getOverrides)
              .stream()
              .flatMap(List::stream)
              .filter(oldOverrideShard -> oldOverrideShard.getIndex() == overrideShard.getIndex())
              .findFirst()
              .map(StackGresShardedClusterShard::getConfigurationForShards)
              .map(StackGresShardedClusterShardConfiguration::getConnectionPoolingConfig)
              .orElse(oldShardsPoolingConfig);
          if (!overrideShardsPoolingConfig.equals(oldOverrideShardsPoolingConfig)) {
            checkIfPoolingConfigExists(review, overrideShardsPoolingConfig,
                "Cannot update shard " + overrideShard.getIndex() + " to pooling config "
                    + overrideShardsPoolingConfig + " because it doesn't exists");
          }
        }
        break;
      }
      default:
    }
  }

  private void checkIfPoolingConfigExists(StackGresShardedClusterReview review,
      String poolingConfig, String onError) throws ValidationFailed {
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    checkIfProvided(poolingConfig, "sgPoolingConfig");
    Optional<StackGresPoolingConfig> poolingConfigOpt = configFinder
        .findByNameAndNamespace(poolingConfig, namespace);

    if (poolingConfigOpt.isEmpty()) {
      fail(onError);
    }
  }

}
