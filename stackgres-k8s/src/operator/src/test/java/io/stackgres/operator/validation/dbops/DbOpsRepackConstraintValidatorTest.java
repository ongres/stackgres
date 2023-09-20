/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import javax.validation.constraints.AssertTrue;

import io.stackgres.common.crd.sgdbops.StackGresDbOpsRepack;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.Test;

class DbOpsRepackConstraintValidatorTest extends ConstraintValidationTest<DbOpsReview> {

  @Override
  protected AbstractConstraintValidator<DbOpsReview> buildValidator() {
    return new DbOpsConstraintValidator();
  }

  @Override
  protected DbOpsReview getValidReview() {
    return AdmissionReviewFixtures.dbOps().loadRepackCreate().get();
  }

  @Override
  protected DbOpsReview getInvalidReview() {
    final DbOpsReview review = AdmissionReviewFixtures.dbOps().loadRepackCreate().get();

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullVacuum_shouldPass() throws ValidationFailed {
    DbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setBenchmark(null);

    validator.validate(review);
  }

  @Test
  void wrongWaitTimeout_shouldFail() {
    DbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getRepack().setWaitTimeout("10s");

    checkErrorCause(StackGresDbOpsRepack.class, "spec.repack.waitTimeout",
        "spec.repack.isWaitTimeoutValid", review, AssertTrue.class);
  }

}
