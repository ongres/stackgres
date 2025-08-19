/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.operator.common.StackGresShardedDbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.conciliation.cluster.context.ClusterPostgresVersionContextAppender;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedDbOpsMajorVersionUpgradeValidatorTest {

  private static final String BUGGY_VERSION =
      ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet().stream().findAny().get();

  private ShardedDbOpsMajorVersionUpgradeValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ShardedDbOpsMajorVersionUpgradeValidator();
  }

  @Test
  void givenBuggyMajorVersionOnCreation_shouldFail() {
    final StackGresShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        BUGGY_VERSION);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Do not use PostgreSQL " + BUGGY_VERSION + ". Please, use PostgreSQL 14.4 since it"
        + " fixes an issue with CREATE INDEX CONCURRENTLY and REINDEX CONCURRENTLY that could cause"
        + " silent data corruption of indexes. For more info see"
        + " https://www.postgresql.org/about/news/postgresql-144-released-2470/.",
        resultMessage);
  }

  private StackGresShardedDbOpsReview getCreationReview() {
    return AdmissionReviewFixtures.shardedDbOps().loadMajorVersionUpgradeCreate().get();
  }

}
