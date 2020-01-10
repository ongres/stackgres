/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.validation.BackupReview;
import io.stackgres.operatorframework.ValidationFailed;
import io.stackgres.operatorframework.ValidationPipeline;

@ApplicationScoped
public class BackupValidationPipeline implements ValidationPipeline<BackupReview> {

  private final Validator validator;
  private final Instance<BackupValidator> validators;

  @Inject
  public BackupValidationPipeline(Instance<BackupValidator> validators, Validator validator) {
    this.validators = validators;
    this.validator = validator;
  }

  /**
   * Validate all {@code Validator}s in sequence.
   */
  @Override
  public void validate(BackupReview review) throws ValidationFailed {
    StackGresBackup backup = review.getRequest().getObject();
    if (backup != null) {
      Set<ConstraintViolation<StackGresBackup>> violations = validator.validate(backup);

      if (!violations.isEmpty()) {
        throw new ValidationFailed(violations);
      }
    }

    for (BackupValidator validator : validators) {
      validator.validate(review);
    }
  }

}
