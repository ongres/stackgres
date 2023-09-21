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
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.storages.AwsCredentials;
import io.stackgres.common.crd.storages.AzureBlobStorageCredentials;
import io.stackgres.common.crd.storages.GoogleCloudCredentials;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.validation.DefaultCustomResourceHolder;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class BackupConfigStorageValidator implements BackupConfigValidator {

  private final String errorTypeUri = ErrorType
      .getErrorTypeUri(ErrorType.INVALID_SECRET);

  private final ResourceFinder<Secret> secretFinder;

  private final DefaultCustomResourceHolder<StackGresBackupConfig> holder;

  @Inject
  public BackupConfigStorageValidator(
      ResourceFinder<Secret> secretFinder,
      DefaultCustomResourceHolder<StackGresBackupConfig> holder) {
    this.secretFinder = secretFinder;
    this.holder = holder;
  }

  @Override
  public void validate(BackupConfigReview review) throws ValidationFailed {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      final StackGresBackupConfig object = review.getRequest().getObject();
      if (holder.isDefaultCustomResource(object)) {
        return;
      }
      String namespace = object.getMetadata().getNamespace();
      String storageType = object.getSpec().getStorage().getType();

      if (storageType.equals("s3")
          && object.getSpec()
          .getStorage().getS3() != null) {
        AwsCredentials credentials = object.getSpec()
            .getStorage().getS3().getAwsCredentials();
        checkSecret(namespace, storageType,
            "accessKeyId", credentials.getSecretKeySelectors().getAccessKeyId());
        checkSecret(namespace, storageType,
            "secretAccessKey", credentials.getSecretKeySelectors().getSecretAccessKey());
      }

      if (storageType.equals("s3Compatible")
          && object.getSpec()
          .getStorage().getS3Compatible() != null) {
        AwsCredentials credentials = object.getSpec()
            .getStorage().getS3Compatible().getAwsCredentials();
        checkSecret(namespace, storageType,
            "accessKeyId", credentials.getSecretKeySelectors().getAccessKeyId());
        checkSecret(namespace, storageType,
            "secretAccessKey", credentials.getSecretKeySelectors().getSecretAccessKey());
      }

      if (storageType.equals("azureblob")
          && object.getSpec()
          .getStorage().getAzureBlob() != null) {
        AzureBlobStorageCredentials credentials = object.getSpec()
            .getStorage().getAzureBlob().getAzureCredentials();
        checkSecret(namespace, storageType,
            "account", credentials.getSecretKeySelectors().getStorageAccount());
        checkSecret(namespace, storageType,
            "accessKey", credentials.getSecretKeySelectors().getAccessKey());
      }

      if (storageType.equals("gcs")
          && object.getSpec().getStorage().getGcs() != null
          && object.getSpec().getStorage().getGcs().getGcpCredentials()
          .getSecretKeySelectors() != null) {
        GoogleCloudCredentials credentials = object.getSpec()
            .getStorage().getGcs().getGcpCredentials();
        checkSecret(namespace, storageType,
            "serviceAccountJsonKey",
            credentials.getSecretKeySelectors().getServiceAccountJsonKey());
      }
    }
  }

  private void checkSecret(
      String namespace,
      String storageType,
      String selectorName,
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
