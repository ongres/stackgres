/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.validation.BackupConfigReview;
import io.stackgres.operatorframework.ValidationFailed;
import io.stackgres.operatorframework.ValidationPipeline;

@ApplicationScoped
public class BackupConfigValidationPipeline implements ValidationPipeline<BackupConfigReview> {

  private Instance<BackupConfigValidator> validators;

  @Inject
  public BackupConfigValidationPipeline(Instance<BackupConfigValidator> validators) {
    this.validators = validators;
  }

  @Override
  public void validate(BackupConfigReview review) throws ValidationFailed {

    for (BackupConfigValidator validator : validators) {
      validator.validate(review);
    }

  }
}
