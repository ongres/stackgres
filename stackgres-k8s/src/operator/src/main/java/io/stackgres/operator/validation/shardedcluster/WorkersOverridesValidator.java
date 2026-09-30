/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class WorkersOverridesValidator implements ShardedClusterValidator {

  private final String constraintViolationUri = ErrorType
      .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    StackGresShardedCluster cluster = review.getRequest().getObject();
    if (review.getRequest().getOperation() == Operation.UPDATE
        || review.getRequest().getOperation() == Operation.CREATE) {
      // Workers and query routers live in disjoint index spaces (query routers indexes are
      // shifted by coordinator.queryRouterIndexOffset), so an override of each type may use
      // the same index without overlapping.
      var overridesWorkers = Stream.concat(
          cluster.getSpec().getWorkersOverrides().stream(),
          cluster.getSpec().getQueryRoutersOverrides().stream())
          .toList();
      checkIndexesUniqueness(overridesWorkers);
    }
  }

  private void checkIndexesUniqueness(
      List<StackGresShardedClusterWorker> overridesWorkers) throws ValidationFailed {
    if (overridesWorkers.stream()
        .collect(Collectors.groupingBy(StackGresShardedClusterWorker::getIndex))
        .values()
        .stream()
        .anyMatch(list -> list.size() > 1)) {
      fail(constraintViolationUri, "Workers overrides must contain unique indexes. Entry index or index range can"
          + " not overlap other entries index or index range");
    }
  }

}
