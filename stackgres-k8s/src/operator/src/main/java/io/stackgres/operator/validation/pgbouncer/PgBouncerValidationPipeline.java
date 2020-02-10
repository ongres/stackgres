/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class PgBouncerValidationPipeline implements ValidationPipeline<PgBouncerReview> {

  private final Validator validator;
  private final Instance<PgBouncerValidator> validators;

  @Inject
  public PgBouncerValidationPipeline(Validator validator,
      @Any Instance<PgBouncerValidator> validators) {
    this.validator = validator;
    this.validators = validators;
  }

  @Override
  public void validate(PgBouncerReview review) throws ValidationFailed {
    StackGresPgbouncerConfig pgBouncerConfig = review.getRequest().getObject();
    if (pgBouncerConfig != null) {
      Set<ConstraintViolation<StackGresPgbouncerConfig>> violations = validator.validate(
          pgBouncerConfig);

      if (!violations.isEmpty()) {
        throw new ValidationFailed(violations);
      }
    }

    for (PgBouncerValidator validator : validators) {
      validator.validate(review);
    }
  }
}
