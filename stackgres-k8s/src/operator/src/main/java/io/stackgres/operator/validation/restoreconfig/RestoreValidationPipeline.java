/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operatorframework.ValidationFailed;
import io.stackgres.operatorframework.ValidationPipeline;

@ApplicationScoped
public class RestoreValidationPipeline implements ValidationPipeline<RestoreConfigReview> {

  private Instance<RestoreConfigValidator> validators;

  @Inject
  public RestoreValidationPipeline(@Any Instance<RestoreConfigValidator> validators) {
    this.validators = validators;
  }

  @Override
  public void validate(RestoreConfigReview review) throws ValidationFailed {

    for (RestoreConfigValidator validator : validators) {
      validator.validate(review);
    }

  }
}
