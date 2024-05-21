/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class ObjectStorageTypeValidator implements ObjectStorageValidator {

  private final String errorTypeUri = ErrorType
      .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);

  @Override
  public void validate(StackGresObjectStorageReview review) throws ValidationFailed {
    Operation operation = review.getRequest().getOperation();

    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      final BackupStorage backupStorage = review.getRequest().getObject().getSpec();
      String storageType = backupStorage.getType();

      switch (storageType) {
        case "s3" -> {
          if (backupStorage.getS3() == null) {
            fail("Invalid object storage. If storage type is s3, the s3 property must be set");
          }
          if (backupStorage.getS3Compatible() != null
              || backupStorage.getGcs() != null
              || backupStorage.getAzureBlob() != null) {
            fail(
                "Invalid object storage. If storage type is s3, neither s3Compatible, "
                    + "gcs or azureBlob must be set"
            );
          }
        }
        case "s3Compatible" -> {
          if (backupStorage.getS3Compatible() == null) {
            fail(
                "Invalid object storage. If storage type is s3Compatible, the s3Compatible "
                    + "property must be set"
            );
          }
          if (backupStorage.getS3() != null
              || backupStorage.getGcs() != null
              || backupStorage.getAzureBlob() != null) {
            fail(
                "Invalid object storage. If storage type is s3Compatible, neither s3, gcs or "
                    + "azureBlob must be set"
            );
          }
        }
        case "gcs" -> {
          if (backupStorage.getGcs() == null) {
            fail("Invalid object storage. If storage type is gcs, the gcs property must be set");
          }
          if (backupStorage.getS3() != null
              || backupStorage.getS3Compatible() != null
              || backupStorage.getAzureBlob() != null) {
            fail(
                "Invalid object storage. If storage type is gcs, neither s3, s3Compatible or "
                    + "azureBlob must be set"
            );
          }
        }
        case "azureBlob" -> {
          if (backupStorage.getAzureBlob() == null) {
            fail(
                "Invalid object storage. If storage type is azureBlob, the azureBlob property "
                    + "must be set"
            );
          }
          if (backupStorage.getS3() != null
              || backupStorage.getS3Compatible() != null
              || backupStorage.getGcs() != null) {
            fail(
                "Invalid object storage. If storage type is azureBlob, neither s3, "
                    + "s3Compatible or gcs must be set"
            );
          }
        }
        default -> fail("Invalid storage type " + storageType
            + ", must be s3, s3Compatible, gcs or azureBlob");
      }
    }
  }

  public void fail(String message) throws ValidationFailed {
    fail(errorTypeUri, message);
  }
}
