/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;

import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardBuilder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverridesShardsScriptsConfigValidatorTest {

  private OverridesShardsScriptsConfigValidator validator;

  @BeforeEach
  void setUp() {
    validator = new OverridesShardsScriptsConfigValidator();
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();

    validator.validate(review);
  }

  @Test
  void givenACreationWithDuplicatedId_shouldFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();

    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withManagedSql(review.getRequest().getObject().getSpec().getShards()
            .getManagedSql())
        .build()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0)
        .getManagedSql().getScripts().get(0).setId(11);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script entries must contain unique ids");
  }

  @Test
  void givenACreationWithReservedId_shouldFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = getCreationReview();

    review.getRequest().getObject().getSpec().getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withManagedSql(review.getRequest().getObject().getSpec().getShards()
            .getManagedSql())
        .build()));
    review.getRequest().getObject().getSpec().getShards()
        .getOverrides().get(0)
        .getManagedSql().getScripts().get(0).setId(
            StackGresShardedClusterUtil.LAST_RESERVER_SCRIPT_ID);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script entries must not use reserved ids from 0 to "
            + StackGresShardedClusterUtil.LAST_RESERVER_SCRIPT_ID);
  }

  private StackGresShardedClusterReview getCreationReview() {
    return AdmissionReviewFixtures.shardedCluster()
        .loadCreateWithManagedSql().get();
  }

}
