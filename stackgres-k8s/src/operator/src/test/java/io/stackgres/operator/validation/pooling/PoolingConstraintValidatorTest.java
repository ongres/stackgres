/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import java.util.Map;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerPgbouncerIni;
import io.stackgres.operator.common.StackGresPoolingConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PoolingConstraintValidatorTest extends ConstraintValidationTest<StackGresPoolingConfigReview> {

  @Override
  protected AbstractConstraintValidator<StackGresPoolingConfigReview> buildValidator() {
    return new PoolingConstraintValidator();
  }

  @Override
  protected StackGresPoolingConfigReview getValidReview() {
    return AdmissionReviewFixtures.poolingConfig().loadCreate().get();
  }

  @Override
  protected StackGresPoolingConfigReview getInvalidReview() {
    final StackGresPoolingConfigReview review = AdmissionReviewFixtures.poolingConfig().loadCreate().get();
    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    StackGresPoolingConfigReview review = getInvalidReview();
    checkNotNullErrorCause(StackGresPoolingConfig.class, "spec", review);
  }

  @Test
  void nullPgBouncerConf_shouldFail() {
    StackGresPoolingConfigReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPgBouncer().getPgbouncerIni().setPgbouncer(null);

    checkNotNullErrorCause(StackGresPoolingConfigPgBouncerPgbouncerIni.class,
        "spec.pgBouncer.pgbouncer\\.ini.pgbouncer", review);
  }

  @Test
  void emptyPgBouncerConf_shouldPass() throws Exception {
    StackGresPoolingConfigReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPgBouncer().getPgbouncerIni()
        .setPgbouncer(Map.of());

    validator.validate(review);
  }
}
