/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.utils.ValidationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SgProfileValidationPipelineIt {

  @Inject
  SgProfileValidationPipeline pipeline;


  @Test
  void constraintViolations_shouldBeDetected() {

    SgProfileReview review = getValidReview();

    review.getRequest().getObject().getSpec().setMemory("");

    ValidationUtils
        .assertErrorType(ErrorType.CONSTRAINT_VIOLATION, () -> pipeline.validate(review));

  }

  private SgProfileReview getValidReview() {
    return JsonUtil.readFromJson("sgprofile_allow_request/create.json",
        SgProfileReview.class);
  }

}