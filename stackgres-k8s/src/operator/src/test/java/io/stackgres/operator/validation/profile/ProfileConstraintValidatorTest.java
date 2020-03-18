/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.validation.constraints.NotBlank;

import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileConstraintValidatorTest extends ConstraintValidationTest<SgProfileReview> {

  @Override
  protected ConstraintValidator<SgProfileReview> buildValidator() {
    return new ProfileConstraintValidator();
  }

  @Override
  protected SgProfileReview getValidReview() {
    return JsonUtil.readFromJson("sgprofile_allow_request/create.json",
        SgProfileReview.class);
  }

  @Override
  protected SgProfileReview getInvalidReview() {
    final SgProfileReview review = JsonUtil.readFromJson("sgprofile_allow_request/create.json",
        SgProfileReview.class);
    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    SgProfileReview review = getInvalidReview();

    checkNotNullErrorCause(StackGresProfile.class, "spec", review);
  }

  @Test
  void nullCpu_shouldFail() {

    SgProfileReview review = getValidReview();
    review.getRequest().getObject().getSpec().setCpu(null);

    checkErrorCause(StackGresProfileSpec.class, "spec.cpu", review, NotBlank.class);

  }

  @Test
  void blankCpu_shouldFail() {
    SgProfileReview review = getValidReview();
    review.getRequest().getObject().getSpec().setCpu("");

    checkErrorCause(StackGresProfileSpec.class, "spec.cpu", review, NotBlank.class);

  }

  @Test
  void blankMemory_shouldFail() {

    SgProfileReview review = getValidReview();
    review.getRequest().getObject().getSpec().setMemory("");

    checkErrorCause(StackGresProfileSpec.class, "spec.memory", review, NotBlank.class);

  }

  @Test
  void nullMemory_shouldFail() {

    SgProfileReview review = getValidReview();
    review.getRequest().getObject().getSpec().setMemory("");

    checkErrorCause(StackGresProfileSpec.class, "spec.memory", review, NotBlank.class);

  }
}