/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.validation.PgConfigReview;
import io.stackgres.operatorframework.ValidationFailed;
import io.stackgres.operatorframework.ValidationPipeline;

@ApplicationScoped
public class PgConfigValidationPipeline implements ValidationPipeline<PgConfigReview> {

  private Instance<PgConfigValidator> validators;

  @Inject
  public PgConfigValidationPipeline(Instance<PgConfigValidator> validators) {
    this.validators = validators;
  }

  @Override
  public void validate(PgConfigReview review) throws ValidationFailed {

    for (PgConfigValidator validator : validators) {
      validator.validate(review);
    }

  }
}
