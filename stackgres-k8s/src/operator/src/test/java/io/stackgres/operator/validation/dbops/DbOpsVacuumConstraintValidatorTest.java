/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;

class DbOpsVacuumConstraintValidatorTest extends ConstraintValidationTest<StackGresDbOpsReview> {

  @Override
  protected ConstraintValidator<StackGresDbOpsReview> buildValidator() {
    return new DbOpsConstraintValidator();
  }

  @Override
  protected StackGresDbOpsReview getValidReview() {
    return JsonUtil.readFromJson("dbops_allow_requests/valid_vacuum_creation.json",
        StackGresDbOpsReview.class);
  }

  @Override
  protected StackGresDbOpsReview getInvalidReview() {
    final StackGresDbOpsReview review = JsonUtil
        .readFromJson("dbops_allow_requests/valid_vacuum_creation.json",
            StackGresDbOpsReview.class);

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullVacuum_shouldPass() throws ValidationFailed {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setBenchmark(null);

    validator.validate(review);
  }

}
