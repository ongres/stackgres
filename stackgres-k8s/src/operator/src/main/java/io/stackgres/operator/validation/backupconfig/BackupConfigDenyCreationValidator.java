/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_CREATION)
public class BackupConfigDenyCreationValidator implements BackupConfigValidator {

  private final String errorTypeUri = ErrorType.getErrorTypeUri(ErrorType.FORBIDDEN_CR_CREATION);

  @Override
  public void validate(BackupConfigReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      fail(StackGresBackupConfig.KIND + " is deprecated, please use a "
          + StackGresObjectStorage.KIND
          + " in the .spec.configurations.backups section of a cluster.");
    }
  }

  public void fail(String message) throws ValidationFailed {
    fail(errorTypeUri, message);
  }
}
