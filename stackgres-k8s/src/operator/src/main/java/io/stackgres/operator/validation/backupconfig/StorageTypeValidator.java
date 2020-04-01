/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class StorageTypeValidator implements BackupConfigValidator {

  private String errorTypeUri;

  @Inject
  public StorageTypeValidator(ConfigContext context) {
    errorTypeUri = context.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);
  }

  @Override
  public void validate(BackupConfigReview review) throws ValidationFailed {

    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {

      String storageType = review.getRequest().getObject().getSpec().getStorage().getType();

      if (storageType.equals("s3")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3() == null) {
        final String message = "Invalid backup configuration,"
            + " source s3 must be set when source type is s3";
        fail(message);
      }

      if (storageType.equals("s3")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3().getBucket() == null) {
        final String message = "Invalid backup configuration,"
            + " source s3 bucket must be set when source type is s3";
        fail(message);
      }

      if (storageType.equals("s3")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3().getAwsCredentials() == null) {
        final String message = "Invalid backup configuration,"
            + " source s3 credentials must be set when source type is s3";
        fail(message);
      }

      if (storageType.equals("s3Compatible")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3Compatible() == null) {
        final String message = "Invalid backup configuration,"
            + " source s3Compatible must be set when source type is s3Compatible";
        fail(message);
      }

      if (storageType.equals("s3Compatible")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3Compatible().getBucket() == null) {
        final String message = "Invalid backup configuration,"
            + " source s3Compatible bucket must be set when source type is s3Compatible";
        fail(message);
      }

      if (storageType.equals("s3Compatible")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3Compatible().getAwsCredentials() == null) {
        final String message = "Invalid backup configuration,"
            + " source s3Compatible credentials must be set when source type is s3Compatible";
        fail(message);
      }

      if (storageType.equals("gcs")
          && review.getRequest().getObject().getSpec()
          .getStorage().getGcs() == null) {
        final String message = "Invalid backup configuration,"
            + " source gcs must be set when source type is gcs";
        fail(message);
      }

      if (storageType.equals("gcs")
          && review.getRequest().getObject().getSpec()
          .getStorage().getGcs().getBucket() == null) {
        final String message = "Invalid backup configuration,"
            + " source gcs bucket must be set when source type is gcs";
        fail(message);
      }

      if (storageType.equals("gcs")
          && review.getRequest().getObject().getSpec()
          .getStorage().getGcs().getCredentials() == null) {
        final String message = "Invalid backup configuration,"
            + " source gcs credentials must be set when source type is gcs";
        fail(message);
      }

      if (storageType.equals("azureblob")
          && review.getRequest().getObject().getSpec()
          .getStorage().getAzureblob() == null) {
        final String message = "Invalid backup configuration,"
            + " source azureblob must be set when source type is azureblob";
        fail(message);
      }

      if (storageType.equals("azureblob")
          && review.getRequest().getObject().getSpec()
          .getStorage().getAzureblob().getBucket() == null) {
        final String message = "Invalid backup configuration,"
            + " source azureblob bucket must be set when source type is azureblob";
        fail(message);
      }

      if (storageType.equals("azureblob")
          && review.getRequest().getObject().getSpec()
          .getStorage().getAzureblob().getAzureCredentials() == null) {
        final String message = "Invalid backup configuration,"
            + " source azureblob credentials must be set when source type is azureblob";
        fail(message);
      }

      if (ImmutableList.of("s3Compatible", "gcs", "azureblob").contains(storageType)
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3() != null) {
        final String message = "Invalid backup configuration,"
            + " source s3 must not be set when source type is " + storageType;
        fail(message);
      }

      if (ImmutableList.of("s3", "gcs", "azureblob").contains(storageType)
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3Compatible() != null) {
        final String message = "Invalid backup configuration,"
            + " source s3Compatible must not be set when source type is " + storageType;
        fail(message);
      }

      if (ImmutableList.of("s3", "s3Compatible", "azureblob").contains(storageType)
          && review.getRequest().getObject().getSpec()
          .getStorage().getGcs() != null) {
        final String message = "Invalid backup configuration,"
            + " source gcs must not be set when source type is " + storageType;
        fail(message);
      }

      if (ImmutableList.of("s3", "s3Compatible", "gcs").contains(storageType)
          && review.getRequest().getObject().getSpec()
          .getStorage().getAzureblob() != null) {
        final String message = "Invalid backup configuration,"
            + " source azureblob must not be set when source type is " + storageType;
        fail(message);
      }

    }
  }

  public void fail(String message) throws ValidationFailed {
    fail(errorTypeUri, message);
  }
}
