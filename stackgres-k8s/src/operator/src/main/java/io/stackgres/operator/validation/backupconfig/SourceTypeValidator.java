/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.validation.BackupConfigReview;
import io.stackgres.operatorframework.Operation;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class SourceTypeValidator implements BackupConfigValidator {

  @Override
  public void validate(BackupConfigReview review) throws ValidationFailed {

    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {

      String storageType = review.getRequest().getObject().getSpec().getStorage().getType();

      if (storageType.equals("volume")
          && review.getRequest().getObject().getSpec()
          .getStorage().getVolume() == null) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source volume must be set when source type is volume");
      }

      if (storageType.equals("s3")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3() == null) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source s3 must be set when source type is s3");
      }

      if (storageType.equals("s3")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3().getPrefix() == null) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source s3 prefix must be set when source type is s3");
      }

      if (storageType.equals("s3")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3().getCredentials() == null) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source s3 credentials must be set when source type is s3");
      }

      if (storageType.equals("gcs")
          && review.getRequest().getObject().getSpec()
          .getStorage().getGcs() == null) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source gcs must be set when source type is gcs");
      }

      if (storageType.equals("gcs")
          && review.getRequest().getObject().getSpec()
          .getStorage().getGcs().getPrefix() == null) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source gcs prefix must be set when source type is gcs");
      }

      if (storageType.equals("gcs")
          && review.getRequest().getObject().getSpec()
          .getStorage().getGcs().getCredentials() == null) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source gcs credentials must be set when source type is gcs");
      }

      if (storageType.equals("azureblob")
          && review.getRequest().getObject().getSpec()
          .getStorage().getAzureblob() == null) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source azureblob must be set when source type is azureblob");
      }

      if (storageType.equals("azureblob")
          && review.getRequest().getObject().getSpec()
          .getStorage().getAzureblob().getPrefix() == null) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source azureblob prefix must be set when source type is azureblob");
      }

      if (storageType.equals("azureblob")
          && review.getRequest().getObject().getSpec()
          .getStorage().getAzureblob().getCredentials() == null) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source azureblob credentials must be set when source type is azureblob");
      }

    }
  }
}
