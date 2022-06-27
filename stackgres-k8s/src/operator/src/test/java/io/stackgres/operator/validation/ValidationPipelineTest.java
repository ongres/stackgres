/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.Collections;

import io.fabric8.kubernetes.api.model.authentication.UserInfo;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ErrorType;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class ValidationPipelineTest<R extends CustomResource<?, ?>,
    T extends AdmissionReview<R>> {

  private UserInfo storageVersionUser;

  @BeforeEach
  public void setUp() {
    storageVersionUser = new UserInfo();
    storageVersionUser.setUsername("system:storageversionmigrator");
    storageVersionUser.setGroups(Collections.singletonList("system:authenticated"));
    storageVersionUser.setUid("uid:system:storageversionmigrator");
  }

  public abstract T getConstraintViolatingReview();

  public abstract ValidationPipeline<T> getPipeline();

  @Test
  void constraintViolations_shouldBeDetected() {
    T review = getConstraintViolatingReview();

    ValidationUtils
        .assertErrorType(ErrorType.CONSTRAINT_VIOLATION, () -> getPipeline().validate(review));
  }

  @Test
  void updatesWithNoChanges_shouldBeValidated() throws ValidationFailed {
    T review = getConstraintViolatingReview();
    review.getRequest().setOperation(Operation.UPDATE);
    review.getRequest().setOldObject(getConstraintViolatingReview().getRequest().getObject());
    getPipeline().validate(review);
  }

}
