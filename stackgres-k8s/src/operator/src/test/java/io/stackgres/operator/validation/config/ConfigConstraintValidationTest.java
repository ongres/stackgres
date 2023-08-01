/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.config;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.common.ConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigConstraintValidationTest extends ConstraintValidationTest<ConfigReview> {

  @Override
  protected ConstraintValidator<ConfigReview> buildValidator() {
    return new ConfigConstraintValidation();
  }

  @Override
  protected ConfigReview getValidReview() {
    return AdmissionReviewFixtures.config().loadCreate().get();
  }

  @Override
  protected ConfigReview getInvalidReview() {
    final ConfigReview backupReview = AdmissionReviewFixtures.config().loadCreate().get();
    backupReview.getRequest().getObject().setSpec(null);
    return backupReview;
  }

  @Test
  void nullSpec_shouldFail() {
    final ConfigReview backupReview = getInvalidReview();

    checkNotNullErrorCause(StackGresConfig.class, "spec", backupReview);
  }

}
