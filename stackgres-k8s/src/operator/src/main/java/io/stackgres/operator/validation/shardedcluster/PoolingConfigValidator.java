/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
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
            .getConfiguration().getConnectionPoolingConfig();
        checkIfPoolingConfigExists(review, coordinatorPoolingConfig,
            "Pooling config " + coordinatorPoolingConfig
                + " not found for coordinator");
        String shardsPoolingConfig = cluster.getSpec().getShards()
            .getConfiguration().getConnectionPoolingConfig();
        checkIfPoolingConfigExists(review, shardsPoolingConfig,
            "Pooling config " + shardsPoolingConfig
                + " not found for shards");
        break;
      }
      case UPDATE: {
        StackGresShardedCluster cluster = review.getRequest().getObject();
        String coordinatorPoolingConfig = cluster.getSpec().getCoordinator()
            .getConfiguration().getConnectionPoolingConfig();
        checkIfPoolingConfigExists(review, coordinatorPoolingConfig,
            "Cannot update coordinator to pooling config "
                + coordinatorPoolingConfig + " because it doesn't exists");
        String shardsPoolingConfig = cluster.getSpec().getShards()
            .getConfiguration().getConnectionPoolingConfig();
        checkIfPoolingConfigExists(review, shardsPoolingConfig,
            "Cannot update shards to pooling config "
                + shardsPoolingConfig + " because it doesn't exists");
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
