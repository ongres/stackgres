/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import java.util.function.Predicate;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.crd.sgshardeddbops.ShardedDbOpsOperation;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ShardedReshardingValidator implements ShardedDbOpsValidator {

  private final CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  @Inject
  public ShardedReshardingValidator(
      CustomResourceFinder<StackGresShardedCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void validate(StackGresShardedDbOpsReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.CREATE
        && ShardedDbOpsOperation.RESHARDING.toString().equals(
            review.getRequest().getObject().getSpec().getOp())) {
      String cluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
      String namespace = review.getRequest().getObject().getMetadata().getNamespace();
      var invalidShardingType = clusterFinder.findByNameAndNamespace(cluster, namespace)
          .map(StackGresShardedCluster::getSpec)
          .map(StackGresShardedClusterSpec::getType)
          .filter(Predicate.not(StackGresShardingType.CITUS.toString()::equals));
      if (invalidShardingType.isPresent()) {
        fail("Reshadring not implemented for SGShardedCluster of type "
            + invalidShardingType.get().toString());
      }
    }
  }

}
