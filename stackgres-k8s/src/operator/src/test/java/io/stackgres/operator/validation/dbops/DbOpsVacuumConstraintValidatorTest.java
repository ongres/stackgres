/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.Test;

class DbOpsVacuumConstraintValidatorTest extends ConstraintValidationTest<DbOpsReview> {

  @Override
  protected ConstraintValidator<DbOpsReview> buildValidator() {
    return new DbOpsConstraintValidator();
  }

  @Override
  protected DbOpsReview getValidReview() {
    return AdmissionReviewFixtures.dbOps().loadVacuumCreate().get();
  }

  @Override
  protected DbOpsReview getInvalidReview() {
    final DbOpsReview review = AdmissionReviewFixtures.dbOps().loadVacuumCreate().get();

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullVacuum_shouldPass() throws ValidationFailed {

    DbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setBenchmark(null);

    validator.validate(review);
  }

}
