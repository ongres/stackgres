/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@ApplicationScoped
public class ModificationValidator implements BackupConfigValidator {

  @Override
  public void validate(BackupConfigReview review) throws ValidationFailed {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.UPDATE) {
      if (!Objects.equals(review.getRequest().getOldObject().getSpec().getPgpConfiguration(),
          review.getRequest().getObject().getSpec().getPgpConfiguration())) {
        throw new ValidationFailed("Modification of pgp configuration is not allowed");
      }

      if (!review.getRequest().getOldObject().getSpec().getStorage().getType().equals(
          review.getRequest().getObject().getSpec().getStorage().getType())
          || !Objects.equals(review.getRequest().getObject().getSpec().getStorage().getS3(),
          review.getRequest().getOldObject().getSpec().getStorage().getS3())
          || !Objects.equals(review.getRequest().getObject().getSpec().getStorage().getGcs(),
          review.getRequest().getOldObject().getSpec().getStorage().getGcs())
          || !Objects.equals(review.getRequest().getObject().getSpec().getStorage().getAzureblob(),
          review.getRequest().getOldObject().getSpec().getStorage().getAzureblob())) {
        throw new ValidationFailed("Modification of storage is not allowed");
      }
    }
  }
}
