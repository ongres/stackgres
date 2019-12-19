/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operatorframework.ValidationFailed;
import io.stackgres.operatorframework.ValidationPipeline;

@ApplicationScoped
public class SgProfileValidationPipeline implements ValidationPipeline<SgProfileReview> {

  private Instance<SgProfileValidator> validators;

  @Inject
  public SgProfileValidationPipeline(@Any Instance<SgProfileValidator> validators) {
    this.validators = validators;
  }

  @Override
  public void validate(SgProfileReview review) throws ValidationFailed {

    for (SgProfileValidator validator : validators) {
      validator.validate(review);
    }
  }
}
