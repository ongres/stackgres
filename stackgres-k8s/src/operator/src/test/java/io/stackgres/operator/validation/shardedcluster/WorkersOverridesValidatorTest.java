/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorkerBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresWorkerType;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkersOverridesValidatorTest {

  private WorkersOverridesValidator validator;

  @BeforeEach
  void setUp() {
    validator = new WorkersOverridesValidator();
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getWorkers().setOverrides(List.of(
        new StackGresShardedClusterWorkerBuilder()
        .withIndex(0)
        .build(),
        new StackGresShardedClusterWorkerBuilder()
        .withIndex(1)
        .build()));

    validator.validate(review);
  }

  @Test
  void givenACreationWithDuplicatedId_shouldFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();

    review.getRequest().getObject().getSpec().getWorkers().setOverrides(List.of(
        new StackGresShardedClusterWorkerBuilder()
        .withIndex(0)
        .build(),
        new StackGresShardedClusterWorkerBuilder()
        .withIndex(0)
        .build()));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Workers overrides must contain unique indexes."
        + " Entry index or index range can not overlap other entries index or index range");
  }

  @Test
  void givenACreationWithSameIndexForAWorkerAndAQueryRouter_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getCoordinator().setQueryRouterClusters(1);
    review.getRequest().getObject().getSpec().getWorkers().setOverrides(List.of(
        new StackGresShardedClusterWorkerBuilder()
        .withIndex(0)
        .withType(StackGresWorkerType.WORKER.toString())
        .build(),
        new StackGresShardedClusterWorkerBuilder()
        .withIndex(0)
        .withType(StackGresWorkerType.QUERY_ROUTER.toString())
        .build()));

    validator.validate(review);
  }

  @Test
  void givenACreationWithDuplicatedQueryRouterId_shouldFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getCoordinator().setQueryRouterClusters(1);
    review.getRequest().getObject().getSpec().getWorkers().setOverrides(List.of(
        new StackGresShardedClusterWorkerBuilder()
        .withIndex(0)
        .withType(StackGresWorkerType.QUERY_ROUTER.toString())
        .build(),
        new StackGresShardedClusterWorkerBuilder()
        .withIndex(0)
        .withType(StackGresWorkerType.QUERY_ROUTER.toString())
        .build()));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Workers overrides must contain unique indexes."
        + " Entry index or index range can not overlap other entries index or index range");
  }

  private StackGresShardedClusterReview getCreationReview() {
    return AdmissionReviewFixtures.shardedCluster()
        .loadCreateWithManagedSql().get();
  }

}
