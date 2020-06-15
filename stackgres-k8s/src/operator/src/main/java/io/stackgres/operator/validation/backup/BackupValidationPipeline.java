/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.validation.SimpleValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class BackupValidationPipeline implements ValidationPipeline<BackupReview> {

  private SimpleValidationPipeline<BackupReview, BackupValidator> pipeline;

  /**
   * Validate all {@code Validator}s in sequence.
   */
  @Override
  public void validate(BackupReview review) throws ValidationFailed {
    pipeline.validate(review);
  }

  @Inject
  public void setValidators(@Any Instance<BackupValidator> validators) {
    this.pipeline = new SimpleValidationPipeline<>(validators);
  }

}
