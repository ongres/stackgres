/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.validation.constraints.AssertTrue;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileConstraintValidatorTest extends ConstraintValidationTest<SgProfileReview> {

  @Override
  protected AbstractConstraintValidator<SgProfileReview> buildValidator() {
    return new ProfileConstraintValidator();
  }

  @Override
  protected SgProfileReview getValidReview() {
    return AdmissionReviewFixtures.instanceProfile().loadCreate().get();
  }

  @Override
  protected SgProfileReview getInvalidReview() {
    final SgProfileReview review = AdmissionReviewFixtures.instanceProfile().loadCreate().get();
    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    SgProfileReview review = getInvalidReview();

    checkNotNullErrorCause(StackGresProfile.class, "spec", review);
  }

  @Test
  void givenCorrectHugePages_shouldNotFail() throws ValidationFailed {
    SgProfileReview review = getValidReview();
    review.getRequest().getObject().getSpec().setHugePages(new StackGresProfileHugePages());
    review.getRequest().getObject().getSpec().getHugePages().setHugepages2Mi("256Mi");

    validator.validate(review);
  }

  @Test
  void givenHugePagesHigherThanMemory_shouldFail() {
    SgProfileReview review = getValidReview();
    review.getRequest().getObject().getSpec().setHugePages(new StackGresProfileHugePages());
    review.getRequest().getObject().getSpec().setMemory("1Gi");
    review.getRequest().getObject().getSpec().getHugePages().setHugepages2Mi("512Mi");
    review.getRequest().getObject().getSpec().getHugePages().setHugepages1Gi("1Gi");

    checkErrorCause(StackGresProfileSpec.class,
        "spec.memory",
        "isMemoryGreaterOrEqualsToSumOfHugePages",
        review, AssertTrue.class,
        "memory can not be less than the sum of all hugepages");
  }

  @Test
  void givenMissingHugePagesValueSet_shouldFail() {
    SgProfileReview review = getValidReview();
    review.getRequest().getObject().getSpec().setHugePages(new StackGresProfileHugePages());

    checkErrorCause(StackGresProfileHugePages.class,
        new String[] { "spec.hugePages.hugepages-2Mi", "spec.hugePages.hugepages-1Gi" },
        "isAnyHugePagesValueSet",
        review, AssertTrue.class,
        "At least one of hugepages-2Mi or hugepages-1Gi must set");
  }

  @Test
  void givenHugePages2MiNotMultipleOf2Mi_shouldFail() {
    SgProfileReview review = getValidReview();
    review.getRequest().getObject().getSpec().setHugePages(new StackGresProfileHugePages());
    review.getRequest().getObject().getSpec().getHugePages().setHugepages2Mi("3Mi");

    checkErrorCause(StackGresProfileHugePages.class,
        "spec.hugePages.hugepages-2Mi",
        "isHugePages2MiValueMultipleOf2Mi",
        review, AssertTrue.class,
        "hugepages-2Mi must be a multiple of 2Mi");
  }

  @Test
  void givenHugePages1GiNotMultipleOf1Gi_shouldFail() {
    SgProfileReview review = getValidReview();
    review.getRequest().getObject().getSpec().setMemory("2Gi");
    review.getRequest().getObject().getSpec().setHugePages(new StackGresProfileHugePages());
    review.getRequest().getObject().getSpec().getHugePages().setHugepages1Gi("1500Mi");

    checkErrorCause(StackGresProfileHugePages.class,
        "spec.hugePages.hugepages-1Gi",
        "isHugePages1GiValueMultipleOf1Gi",
        review, AssertTrue.class,
        "hugepages-1Gi must be a multiple of 1Gi");
  }

}
