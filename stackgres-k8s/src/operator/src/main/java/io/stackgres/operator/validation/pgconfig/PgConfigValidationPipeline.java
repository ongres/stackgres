/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.validation.SimpleValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class PgConfigValidationPipeline implements ValidationPipeline<PgConfigReview> {

  private SimpleValidationPipeline<PgConfigReview, PgConfigValidator> pipeline;

  @Inject
  public PgConfigValidationPipeline(@Any Instance<PgConfigValidator> validators) {
    this.pipeline = new SimpleValidationPipeline<>(validators);
  }

  @Override
  public void validate(PgConfigReview review) throws ValidationFailed {
    pipeline.validate(review);
  }
}
