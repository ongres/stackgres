/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class BackupNameValidator implements BackupValidator {

  private String errorTypeUri;

  @Inject
  public BackupNameValidator(ConfigContext context) {
    errorTypeUri = context.getErrorTypeUri(ErrorType.FORBIDDEN_CR_UPDATE);
  }

  @Override
  public void validate(BackupReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      String name = Optional.ofNullable(review.getRequest().getObject())
          .map(StackGresBackup::getStatus)
          .map(StackGresBackupStatus::getInternalName)
          .orElse(null);
      String oldName = Optional.ofNullable(review.getRequest().getOldObject())
          .map(StackGresBackup::getStatus)
          .map(StackGresBackupStatus::getInternalName)
          .orElse(null);
      if (oldName != null && !Objects.equals(oldName, name)) {
        final String message = "Update of backups name is forbidden";
        fail(errorTypeUri, message);
      }
    }

  }

}
