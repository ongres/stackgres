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
      if (!review.getRequest().getOldObject().getSpec().getStorage().getType().equals(
          review.getRequest().getObject().getSpec().getStorage().getType())
          || !Objects.equals(review.getRequest().getObject().getSpec().getStorage().getS3(),
          review.getRequest().getOldObject().getSpec().getStorage().getS3())
          || !Objects.equals(review.getRequest().getObject().getSpec().getStorage().getGcs(),
          review.getRequest().getOldObject().getSpec().getStorage().getGcs())
          || !Objects.equals(review.getRequest().getObject().getSpec().getStorage().getAzureblob(),
          review.getRequest().getOldObject().getSpec().getStorage().getAzureblob())) {
        fail("Modification of storage is not allowed");
      }
    }
  }

  public void fail(String message) throws ValidationFailed {
    fail(errorTypeUri, message);
  }
}
