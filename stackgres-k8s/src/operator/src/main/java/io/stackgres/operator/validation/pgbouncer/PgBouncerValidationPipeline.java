/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.validation.SimpleValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class PgBouncerValidationPipeline
    implements ValidationPipeline<PgBouncerReview> {

  private SimpleValidationPipeline<PgBouncerReview, PgBouncerValidator> genericPipeline;

  private Instance<PgBouncerValidator> validatorInstances;

  @PostConstruct
  public void init() {
    genericPipeline = new SimpleValidationPipeline<>(validatorInstances);
  }

  @Inject
  public void setValidatorInstances(@Any Instance<PgBouncerValidator> validatorInstances) {
    this.validatorInstances = validatorInstances;
  }

  @Override
  public void validate(PgBouncerReview review) throws ValidationFailed {
    genericPipeline.validate(review);
  }
}
