/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.validation.SimpleValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class SgProfileValidationPipeline implements ValidationPipeline<SgProfileReview> {

  private SimpleValidationPipeline<SgProfileReview, SgProfileValidator> pipeline;

  private Instance<SgProfileValidator> validators;

  @PostConstruct
  public void init() {
    pipeline = new SimpleValidationPipeline<>(validators);
  }

  @Override
  public void validate(SgProfileReview review) throws ValidationFailed {

    pipeline.validate(review);

  }

  @Inject
  public void setValidators(@Any Instance<SgProfileValidator> validators) {
    this.validators = validators;
  }
}
