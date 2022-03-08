/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import java.util.Map;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerPgbouncerIni;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PoolingConstraintValidatorTest extends ConstraintValidationTest<PoolingReview> {

  @Override
  protected ConstraintValidator<PoolingReview> buildValidator() {
    return new PoolingConstraintValidator();
  }

  @Override
  protected PoolingReview getValidReview() {
    return AdmissionReviewFixtures.poolingConfig().loadCreate().get();
  }

  @Override
  protected PoolingReview getInvalidReview() {
    final PoolingReview review = AdmissionReviewFixtures.poolingConfig().loadCreate().get();
    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    PoolingReview review = getInvalidReview();
    checkNotNullErrorCause(StackGresPoolingConfig.class, "spec", review);
  }

  @Test
  void nullPgBouncerConf_shouldFail() {
    PoolingReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPgBouncer().getPgbouncerIni().setParameters(null);

    checkNotEmptyErrorCause(StackGresPoolingConfigPgBouncerPgbouncerIni.class,
        "spec.pgBouncer.pgbouncer\\.ini.pgbouncer", review);

  }

  @Test
  void emptyPgBouncerConf_shouldFail() {
    PoolingReview review = getValidReview();
    review.getRequest().getObject().getSpec().getPgBouncer().getPgbouncerIni()
        .setParameters(Map.of());

    checkNotEmptyErrorCause(StackGresPoolingConfigPgBouncerPgbouncerIni.class,
        "spec.pgBouncer.pgbouncer\\.ini.pgbouncer", review);

  }
}
