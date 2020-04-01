/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.validation.SimpleValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class PoolingValidationPipeline
    implements ValidationPipeline<PoolingReview> {

  private SimpleValidationPipeline<PoolingReview, PoolingValidator> genericPipeline;

  private Instance<PoolingValidator> validatorInstances;

  @PostConstruct
  public void init() {
    genericPipeline = new SimpleValidationPipeline<>(validatorInstances);
  }

  @Inject
  public void setValidatorInstances(@Any Instance<PoolingValidator> validatorInstances) {
    this.validatorInstances = validatorInstances;
  }

  @Override
  public void validate(PoolingReview review) throws ValidationFailed {
    genericPipeline.validate(review);
  }
}
