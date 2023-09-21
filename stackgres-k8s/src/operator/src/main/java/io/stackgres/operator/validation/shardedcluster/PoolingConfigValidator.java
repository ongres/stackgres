/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShard;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

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
            .getConfigurations().getSgPoolingConfig();
        checkIfPoolingConfigExists(review, coordinatorPoolingConfig,
            "Pooling config " + coordinatorPoolingConfig
                + " not found for coordinator");
        String shardsPoolingConfig = cluster.getSpec().getShards()
            .getConfigurations().getSgPoolingConfig();
        checkIfPoolingConfigExists(review, shardsPoolingConfig,
            "Pooling config " + shardsPoolingConfig
                + " not found for shards");
        for (var overrideShard : Optional.of(cluster.getSpec().getShards())
            .map(StackGresShardedClusterShards::getOverrides)
            .orElse(List.of())) {
          if (overrideShard.getConfigurationsForShards() == null
              || overrideShard.getConfigurationsForShards().getSgPoolingConfig() == null) {
            continue;
          }
          String overrideShardsPoolingConfig = overrideShard
              .getConfigurationsForShards().getSgPoolingConfig();
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
            .getConfigurations().getSgPoolingConfig();
        String oldCoordinatorPoolingConfig = oldCluster.getSpec().getCoordinator()
            .getConfigurations().getSgPoolingConfig();
        if (!coordinatorPoolingConfig.equals(oldCoordinatorPoolingConfig)) {
          checkIfPoolingConfigExists(review, coordinatorPoolingConfig,
              "Cannot update coordinator to pooling config "
                  + coordinatorPoolingConfig + " because it doesn't exists");
        }
        String shardsPoolingConfig = cluster.getSpec().getShards()
            .getConfigurations().getSgPoolingConfig();
        String oldShardsPoolingConfig = oldCluster.getSpec().getShards()
            .getConfigurations().getSgPoolingConfig();
        if (!shardsPoolingConfig.equals(oldShardsPoolingConfig)) {
          checkIfPoolingConfigExists(review, shardsPoolingConfig,
              "Cannot update shards to pooling config "
                  + shardsPoolingConfig + " because it doesn't exists");
        }
        for (var overrideShard : Optional.of(cluster.getSpec().getShards())
            .map(StackGresShardedClusterShards::getOverrides)
            .orElse(List.of())) {
          if (overrideShard.getConfigurationsForShards() == null
              || overrideShard.getConfigurationsForShards().getSgPoolingConfig() == null) {
            continue;
          }
          String overrideShardsPoolingConfig = overrideShard
              .getConfigurationsForShards().getSgPoolingConfig();
          String oldOverrideShardsPoolingConfig = Optional.of(oldCluster.getSpec().getShards())
              .map(StackGresShardedClusterShards::getOverrides)
              .stream()
              .flatMap(List::stream)
              .filter(oldOverrideShard -> Objects.equals(
                  oldOverrideShard.getIndex(),
                  overrideShard.getIndex()))
              .findFirst()
              .map(StackGresShardedClusterShard::getConfigurationsForShards)
              .map(StackGresShardedClusterShardConfigurations::getSgPoolingConfig)
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
