/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;

class DbOpsConstraintValidatorTest extends ConstraintValidationTest<StackGresDbOpsReview> {

  @Override
  protected ConstraintValidator<StackGresDbOpsReview> buildValidator() {
    return new DbOpsConstraintValidator();
  }

  @Override
  protected StackGresDbOpsReview getValidReview() {
    return JsonUtil.readFromJson("dbops_allow_requests/valid_pgbench_creation.json",
        StackGresDbOpsReview.class);
  }

  @Override
  protected StackGresDbOpsReview getInvalidReview() {
    final StackGresDbOpsReview review = JsonUtil
        .readFromJson("dbops_allow_requests/valid_pgbench_creation.json",
            StackGresDbOpsReview.class);

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().setSpec(null);

    checkNotNullErrorCause(StackGresDbOps.class, "spec", review);
  }

  @Test
  void nullSgCluster_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setSgCluster(null);

    checkErrorCause(StackGresDbOpsSpec.class, "spec.sgCluster",
        review, NotNull.class);
  }

  @Test
  void nullOp_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setOp(null);

    checkErrorCause(StackGresDbOpsSpec.class, "spec.op",
        review, NotNull.class);
  }

  @Test
  void wrongOp_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setOp("test");

    checkErrorCause(StackGresDbOpsSpec.class, "spec.op",
        "spec.isOpValid", review, AssertTrue.class);
  }

  @Test
  void wrongRunAt_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setRunAt("2018-01-01 01:02:03");

    checkErrorCause(StackGresDbOpsSpec.class, "spec.runAt",
        "spec.isRunAtValid", review, AssertTrue.class);
  }

  @Test
  void wrongTimeout_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setTimeout("10s");

    checkErrorCause(StackGresDbOpsSpec.class, "spec.timeout",
        "spec.isTimeoutValid", review, AssertTrue.class);
  }

  @Test
  void negativeTimeout_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setTimeout("-PT1M");

    checkErrorCause(StackGresDbOpsSpec.class, "spec.timeout",
        "spec.isTimeoutValid", review, AssertTrue.class);
  }

  @Test
  void invalidLowMaxRetries_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setMaxRetries(0);

    checkErrorCause(StackGresDbOpsSpec.class, "spec.maxRetries",
        review, Min.class);

  }

  @Test
  void invalidHighMaxRetries_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setMaxRetries(11);

    checkErrorCause(StackGresDbOpsSpec.class, "spec.maxRetries",
        review, Max.class);

  }

}