/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.storages.AwsCredentials;
import io.stackgres.common.crd.storages.AzureBlobStorageCredentials;
import io.stackgres.common.crd.storages.GoogleCloudCredentials;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class BackupConfigStorageValidator implements BackupConfigValidator {

  private final String errorTypeUri = ErrorType
      .getErrorTypeUri(ErrorType.INVALID_SECRET);

  private final ResourceFinder<Secret> secretFinder;

  @Inject
  public BackupConfigStorageValidator(ResourceFinder<Secret> secretFinder) {
    super();
    this.secretFinder = secretFinder;
  }

  @Override
  public void validate(BackupConfigReview review) throws ValidationFailed {

    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {

      String namespace = review.getRequest().getObject().getMetadata().getNamespace();
      String storageType = review.getRequest().getObject().getSpec().getStorage().getType();

      if (storageType.equals("s3")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3() != null) {
        AwsCredentials credentials = review.getRequest().getObject().getSpec()
            .getStorage().getS3().getAwsCredentials();
        checkSecret(namespace, storageType,
            "accessKeyId", credentials.getSecretKeySelectors().getAccessKeyId());
        checkSecret(namespace, storageType,
            "secretAccessKey", credentials.getSecretKeySelectors().getSecretAccessKey());
      }

      if (storageType.equals("s3compatible")
          && review.getRequest().getObject().getSpec()
          .getStorage().getS3Compatible() != null) {
        AwsCredentials credentials = review.getRequest().getObject().getSpec()
            .getStorage().getS3Compatible().getAwsCredentials();
        checkSecret(namespace, storageType,
            "accessKeyId", credentials.getSecretKeySelectors().getAccessKeyId());
        checkSecret(namespace, storageType,
            "secretAccessKey", credentials.getSecretKeySelectors().getSecretAccessKey());
      }

      if (storageType.equals("azureblob")
          && review.getRequest().getObject().getSpec()
          .getStorage().getAzureBlob() != null) {
        AzureBlobStorageCredentials credentials = review.getRequest().getObject().getSpec()
            .getStorage().getAzureBlob().getAzureCredentials();
        checkSecret(namespace, storageType,
            "account", credentials.getSecretKeySelectors().getAccount());
        checkSecret(namespace, storageType,
            "accessKey", credentials.getSecretKeySelectors().getAccessKey());
      }

      if (storageType.equals("gcs")
          && review.getRequest().getObject().getSpec().getStorage().getGcs() != null
          && review.getRequest().getObject().getSpec().getStorage().getGcs().getCredentials()
          .getSecretKeySelectors() != null) {
        GoogleCloudCredentials credentials = review.getRequest().getObject().getSpec()
            .getStorage().getGcs().getCredentials();
        checkSecret(namespace, storageType,
            "serviceAccountJsonKey",
            credentials.getSecretKeySelectors().getServiceAccountJsonKey());
      }
    }
  }

  private void checkSecret(String namespace, String storageType, String selectorName,
      SecretKeySelector secretKeySelector) throws ValidationFailed {
    Optional<Secret> secret = secretFinder.findByNameAndNamespace(
        secretKeySelector.getName(), namespace);
    if (!secret.isPresent()) {
      final String message = "Invalid backup configuration,"
          + " secret " + secretKeySelector.getName()
          + " for " + selectorName + " of " + storageType + " credentials not found";
      fail(message);
    }
    if (secret.map(Secret::getData)
        .filter(data -> data.containsKey(secretKeySelector.getKey()))
        .map(data -> Boolean.FALSE)
        .orElse(true)) {
      final String message = "Invalid backup configuration,"
          + " key " + secretKeySelector.getKey() + " of secret " + secretKeySelector.getName()
          + " for " + selectorName + " of " + storageType + " credentials not found";
      fail(message);
    }
  }

  public void fail(String message) throws ValidationFailed {
    fail(errorTypeUri, message);
  }
}
