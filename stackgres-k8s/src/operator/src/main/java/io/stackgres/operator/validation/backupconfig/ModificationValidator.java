/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class ModificationValidator implements BackupConfigValidator {

  private String errorTypeUri;

  @Inject
  public ModificationValidator(ConfigContext context) {
    errorTypeUri = context.getErrorTypeUri(ErrorType.FORBIDDEN_CR_UPDATE);
  }

  @Override
  public void validate(BackupConfigReview review) throws ValidationFailed {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.UPDATE) {
      final StackGresBackupConfig oldObject = review.getRequest().getOldObject();
      final StackGresBackupConfig object = review.getRequest().getObject();
      if (!oldObject.getSpec().getStorage().getType().equals(// NOPMD
          object.getSpec().getStorage().getType())
          || !Objects.equals(object.getSpec().getStorage().getS3(),
              oldObject.getSpec().getStorage().getS3())
          || !Objects.equals(
              object.getSpec().getStorage().getS3Compatible(),
              oldObject.getSpec().getStorage().getS3Compatible())
          || !Objects.equals(object.getSpec().getStorage().getGcs(),
              oldObject.getSpec().getStorage().getGcs())
          || !Objects.equals(object.getSpec().getStorage().getAzureblob(),
              oldObject.getSpec().getStorage().getAzureblob())) {
        fail("Modification of storage is not allowed");
      }
    }
  }

  public void fail(String message) throws ValidationFailed {
    fail(errorTypeUri, message);
  }
}
