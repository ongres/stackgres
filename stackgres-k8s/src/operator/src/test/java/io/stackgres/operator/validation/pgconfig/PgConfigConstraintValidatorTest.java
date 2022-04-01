/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.HashMap;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgConfigConstraintValidatorTest extends ConstraintValidationTest<PgConfigReview> {

  @Override
  protected ConstraintValidator<PgConfigReview> buildValidator() {
    return new PgConfigConstraintValidator();
  }

  @Override
  protected PgConfigReview getValidReview() {
    return JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig_update.json",
        PgConfigReview.class);
  }

  @Override
  protected PgConfigReview getInvalidReview() {
    final PgConfigReview review =
        JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig_update.json",
            PgConfigReview.class);
    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    PgConfigReview review = getInvalidReview();

    checkNotNullErrorCause(StackGresPostgresConfig.class, "spec", review);

  }

  @Test
  void emptyPosgrestConf_shouldFail() {
    PgConfigReview review = getValidReview();
    review.getRequest().getObject().getSpec().setPostgresqlConf(new HashMap<>());

    checkNotEmptyErrorCause(StackGresPostgresConfigSpec.class, "spec.postgresql\\.conf", review);
  }
}
