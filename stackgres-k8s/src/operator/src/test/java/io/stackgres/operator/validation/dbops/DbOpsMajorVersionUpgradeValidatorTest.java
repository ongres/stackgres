/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.conciliation.cluster.context.ClusterPostgresVersionContextAppender;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsMajorVersionUpgradeValidatorTest {

  private static final String BUGGY_VERSION =
      ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet().stream().findAny().get();

  private DbOpsMajorVersionUpgradeValidator validator;

  @BeforeEach
  void setUp() {
    validator = new DbOpsMajorVersionUpgradeValidator();
  }

  @Test
  void givenBuggyVersionOnCreation_shouldFail() {
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        BUGGY_VERSION);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertTrue(resultMessage, resultMessage.startsWith("Do not use PostgreSQL " + BUGGY_VERSION + "."));
  }

  private StackGresDbOpsReview getCreationReview() {
    return AdmissionReviewFixtures.dbOps().loadMajorVersionUpgradeCreate().get();
  }

}
