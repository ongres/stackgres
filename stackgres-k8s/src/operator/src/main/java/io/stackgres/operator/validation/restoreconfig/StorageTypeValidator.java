/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import java.util.function.Supplier;
import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operator.customresource.storages.BackupStorage;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class StorageTypeValidator implements RestoreConfigValidator {

  @Override
  public void validate(RestoreConfigReview review) throws ValidationFailed {

    switch (review.getRequest().getOperation()) {
      case CREATE:
      case UPDATE:
        BackupStorage storage = review.getRequest().getObject().getSpec().getSource().getStorage();
        if (storage != null) {
          switch (storage.getType()) {
            case "s3":
              failIf(() -> storage.getS3() == null, "If storage type is \"s3\","
                  + " a S3 configuration must be provided");
              break;
            case "gcs":
              failIf(() -> storage.getGcs() == null, "If storage type is \"gcs\", "
                  + "a Google Cloud Storage configuration must be provided");
              break;
            case "azure":
              failIf(() -> storage.getAzureblob() == null, "If storage type is \"azure\", "
                  + "an Azure Blob Storage configuration must be provided");
              break;
            default:
              throw new ValidationFailed("Invalid storage type " + storage.getType());
          }
        }
        break;
      default:
        break;
    }
  }

  private void failIf(Supplier<Boolean> condition, String message) throws ValidationFailed {

    if (condition.get()) {
      throw new ValidationFailed(message);
    }

  }
}
