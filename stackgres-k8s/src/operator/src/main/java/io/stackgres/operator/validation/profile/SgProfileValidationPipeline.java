/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class SgProfileValidationPipeline implements ValidationPipeline<SgProfileReview> {

  private final Validator validator;
  private final Instance<SgProfileValidator> validators;

  @Inject
  public SgProfileValidationPipeline(Validator validator,
      @Any Instance<SgProfileValidator> validators) {
    this.validator = validator;
    this.validators = validators;
  }

  @Override
  public void validate(SgProfileReview review) throws ValidationFailed {
    StackGresProfile profile = review.getRequest().getObject();
    if (profile != null) {
      Set<ConstraintViolation<StackGresProfile>> violations = validator.validate(profile);

      if (!violations.isEmpty()) {
        throw new ValidationFailed(violations);
      }
    }

    for (SgProfileValidator validator : validators) {
      validator.validate(review);
    }
  }
}
