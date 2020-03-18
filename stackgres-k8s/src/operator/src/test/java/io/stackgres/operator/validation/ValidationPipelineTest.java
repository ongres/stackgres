/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.junit.jupiter.api.Test;

public abstract class ValidationPipelineTest<R extends CustomResource, T extends AdmissionReview<R>> {

  @Inject
  ValidationPipeline<T> pipeline;

  public abstract T getConstraintViolatingReview();

  @Test
  void constraintViolations_shouldBeDetected() {
    T review = getConstraintViolatingReview();

    ValidationUtils
        .assertErrorType(ErrorType.CONSTRAINT_VIOLATION, () -> pipeline.validate(review));

  }

}