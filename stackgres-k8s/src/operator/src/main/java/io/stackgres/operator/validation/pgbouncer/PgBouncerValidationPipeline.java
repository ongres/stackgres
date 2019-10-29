/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.validation.PgBouncerReview;
import io.stackgres.operatorframework.ValidationFailed;
import io.stackgres.operatorframework.ValidationPipeline;

@ApplicationScoped
public class PgBouncerValidationPipeline implements ValidationPipeline<PgBouncerReview> {

  private Instance<PgBouncerValidator> validators;

  @Inject
  public PgBouncerValidationPipeline(@Any Instance<PgBouncerValidator> validators) {
    this.validators = validators;
  }

  @Override
  public void validate(PgBouncerReview review) throws ValidationFailed {

    for (PgBouncerValidator validator : validators) {
      validator.validate(review);
    }

  }
}
