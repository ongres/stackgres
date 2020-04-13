/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ConfigLoader;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PgConfigUpdateValidatorTest {

  private PgConfigReview review;

  private ConfigContext context = new ConfigLoader();

  private PgConfigUpdateValidator validator;

  @BeforeEach
  void setUp() throws NoSuchFieldException {
    review = JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig_update.json",
        PgConfigReview.class);

    validator = new PgConfigUpdateValidator(context);
    validator.init();
  }

  @Test
  void ifNoUpdatesOfPgVersion_shouldPass() throws ValidationFailed {

    final AdmissionRequest<StackGresPostgresConfig> request = review.getRequest();
    request.getObject().getSpec().setPostgresVersion("12");
    request.getOldObject().getSpec().setPostgresVersion("12");

    validator.validate(review);
  }

  @Test
  void updatesOfPgVersion_shouldFail() {

    final AdmissionRequest<StackGresPostgresConfig> request = review.getRequest();
    request.getObject().getSpec().setPostgresVersion("12");
    request.getOldObject().getSpec().setPostgresVersion("11");

    ValidationFailed vf = ValidationUtils
        .assertErrorType(ErrorType.FORBIDDEN_CR_UPDATE, () -> validator.validate(review));

    ValidationUtils.checkErrorCause(vf.getResult(),
        "spec.postgresVersion",
        "postgresVersion is not updatable",
        "FieldNotUpdatable");

  }
}