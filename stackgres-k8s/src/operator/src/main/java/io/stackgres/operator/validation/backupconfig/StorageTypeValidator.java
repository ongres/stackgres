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
          .getStorage().getS3().getPrefix() == null) {
        final String message = "Invalid backup configuration,"
            + " source s3 prefix must be set when source type is s3";
        fail(message);
      }

      if (storageType.equals("s3")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3().getCredentials() == null) {
        final String message = "Invalid backup configuration,"
            + " source s3 credentials must be set when source type is s3";
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
          .getStorage().getGcs().getPrefix() == null) {
        final String message = "Invalid backup configuration,"
            + " source gcs prefix must be set when source type is gcs";
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
          .getStorage().getAzureblob().getPrefix() == null) {
        final String message = "Invalid backup configuration,"
            + " source azureblob prefix must be set when source type is azureblob";
        fail(message);
      }

      if (storageType.equals("azureblob")
          && review.getRequest().getObject().getSpec()
          .getStorage().getAzureblob().getCredentials() == null) {
        final String message = "Invalid backup configuration,"
            + " source azureblob credentials must be set when source type is azureblob";
        fail(message);
      }

      if (ImmutableList.of("gcs", "azureblob").contains(storageType)
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3() != null) {
        final String message = "Invalid backup configuration,"
            + " source s3 must not be set when source type is " + storageType;
        fail(message);
      }

      if (ImmutableList.of("s3", "azureblob").contains(storageType)
          && review.getRequest().getObject().getSpec()
          .getStorage().getGcs() != null) {
        final String message = "Invalid backup configuration,"
            + " source gcs must not be set when source type is " + storageType;
        fail(message);
      }

      if (ImmutableList.of("s3", "gcs").contains(storageType)
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
