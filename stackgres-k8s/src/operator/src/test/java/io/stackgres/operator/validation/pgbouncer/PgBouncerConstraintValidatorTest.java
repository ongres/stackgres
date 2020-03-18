/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import java.util.HashMap;

import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigSpec;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PgBouncerConstraintValidatorTest extends ConstraintValidationTest<PgBouncerReview> {

  @Override
  protected ConstraintValidator<PgBouncerReview> buildValidator() {
    return new PgBouncerConstraintValidator();
  }

  @Override
  protected PgBouncerReview getValidReview() {
    return JsonUtil.readFromJson("pgbouncer_allow_request/create.json",
        PgBouncerReview.class);
  }

  @Override
  protected PgBouncerReview getInvalidReview() {
    final PgBouncerReview review = JsonUtil.readFromJson("pgbouncer_allow_request/create.json",
        PgBouncerReview.class);
    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {

    PgBouncerReview review = getInvalidReview();

    checkNotNullErrorCause(StackGresPgbouncerConfig.class, "spec", review);

  }

  @Test
  void nullPgBouncerConf_shouldFail() {

    PgBouncerReview review = getValidReview();
    review.getRequest().getObject().getSpec().setPgbouncerConf(null);

    checkNotEmptyErrorCause(StackGresPgbouncerConfigSpec.class, "spec.pgbouncerConf", review);

  }


  @Test
  void emptyPgBouncerConf_shouldFail() {

    PgBouncerReview review = getValidReview();
    review.getRequest().getObject().getSpec().setPgbouncerConf(new HashMap<>());

    checkNotEmptyErrorCause(StackGresPgbouncerConfigSpec.class, "spec.pgbouncerConf", review);

  }
}