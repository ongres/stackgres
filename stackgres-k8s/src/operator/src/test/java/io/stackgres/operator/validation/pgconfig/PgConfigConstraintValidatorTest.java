/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.HashMap;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgConfigConstraintValidatorTest extends ConstraintValidationTest<StackGresPostgresConfigReview> {

  @Override
  protected AbstractConstraintValidator<StackGresPostgresConfigReview> buildValidator() {
    return new PgConfigConstraintValidator();
  }

  @Override
  protected StackGresPostgresConfigReview getValidReview() {
    return AdmissionReviewFixtures.postgresConfig().loadUpdate().get();
  }

  @Override
  protected StackGresPostgresConfigReview getInvalidReview() {
    final StackGresPostgresConfigReview review = AdmissionReviewFixtures.postgresConfig().loadUpdate().get();
    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    StackGresPostgresConfigReview review = getInvalidReview();

    checkNotNullErrorCause(StackGresPostgresConfig.class, "spec", review);

  }

  @Test
  void nullPosgrestConf_shouldFail() {
    StackGresPostgresConfigReview review = getValidReview();
    review.getRequest().getObject().getSpec().setPostgresqlConf(null);

    checkNotNullErrorCause(StackGresPostgresConfigSpec.class, "spec.postgresql\\.conf", review);
  }

  @Test
  void emptyPosgrestConf_shouldPass() throws Exception {
    StackGresPostgresConfigReview review = getValidReview();
    review.getRequest().getObject().getSpec().setPostgresqlConf(new HashMap<>());

    validator.validate(review);
  }
}
