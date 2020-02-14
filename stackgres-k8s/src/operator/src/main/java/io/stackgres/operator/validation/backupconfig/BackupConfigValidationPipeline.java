/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class BackupConfigValidationPipeline implements ValidationPipeline<BackupConfigReview> {

  private final Validator validator;
  private final Instance<BackupConfigValidator> validators;

  @Inject
  public BackupConfigValidationPipeline(Validator validator,
      Instance<BackupConfigValidator> validators) {
    this.validator = validator;
    this.validators = validators;
  }

  @Override
  public void validate(BackupConfigReview review) throws ValidationFailed {
    StackGresBackupConfig backupConfig = review.getRequest().getObject();
    if (backupConfig != null) {
      Set<ConstraintViolation<StackGresBackupConfig>> violations = validator.validate(backupConfig);

      if (!violations.isEmpty()) {
        throw new ValidationFailed(violations);
      }
    }

    for (BackupConfigValidator validator : validators) {
      validator.validate(review);
    }
  }
}
