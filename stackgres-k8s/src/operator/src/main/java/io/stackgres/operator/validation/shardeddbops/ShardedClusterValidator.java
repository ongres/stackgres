/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ShardedClusterValidator implements ShardedDbOpsValidator {

  private final CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  @Inject
  public ShardedClusterValidator(
      CustomResourceFinder<StackGresShardedCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void validate(ShardedDbOpsReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      String cluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
      String namespace = review.getRequest().getObject().getMetadata().getNamespace();
      checkIfClusterExists(cluster, namespace,
          "SGShardedCluster " + cluster + " not found");
    }
  }

  private void checkIfClusterExists(String cluster, String namespace,
      String onError) throws ValidationFailed {
    Optional<StackGresShardedCluster> clusterOpt = clusterFinder
        .findByNameAndNamespace(cluster, namespace);

    if (!clusterOpt.isPresent()) {
      fail(onError);
    }
  }

}
