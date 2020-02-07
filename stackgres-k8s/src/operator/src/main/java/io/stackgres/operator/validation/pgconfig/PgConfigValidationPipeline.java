/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class PgConfigValidationPipeline implements ValidationPipeline<PgConfigReview> {

  private final Validator validator;
  private final Instance<PgConfigValidator> validators;

  @Inject
  public PgConfigValidationPipeline(Validator validator, Instance<PgConfigValidator> validators) {
    this.validator = validator;
    this.validators = validators;
  }

  @Override
  public void validate(PgConfigReview review) throws ValidationFailed {
    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    if (pgConfig != null) {
      Set<ConstraintViolation<StackGresPostgresConfig>> violations = validator.validate(pgConfig);

      if (!violations.isEmpty()) {
        throw new ValidationFailed(violations);
      }
    }

    for (PgConfigValidator validator : validators) {
      validator.validate(review);
    }
  }
}
