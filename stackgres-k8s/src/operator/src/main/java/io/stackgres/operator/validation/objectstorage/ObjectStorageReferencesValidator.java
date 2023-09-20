/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.storages.AwsSecretKeySelector;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.validation.DefaultCustomResourceHolder;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_SECRET)
public class ObjectStorageReferencesValidator implements ObjectStorageValidator {

  private static final String MISSING_SECRET_MESSAGE_FORMAT =
      "Invalid object storage, secret %s not found for selector name %s of storage type %s";

  private static final String MISSING_SECRET_KEY_MESSAGE_FORMAT = "Invalid object storage, "
      + "key %s not found in secret %s for selector name %s of storage type %s";

  private final String errorTypeUri = ErrorType
      .getErrorTypeUri(ErrorType.INVALID_SECRET);

  private final ResourceFinder<Secret> secretFinder;

  private final DefaultCustomResourceHolder<StackGresObjectStorage> holder;

  @Inject
  public ObjectStorageReferencesValidator(
      ResourceFinder<Secret> secretFinder,
      DefaultCustomResourceHolder<StackGresObjectStorage> holder) {
    this.secretFinder = secretFinder;
    this.holder = holder;
  }

  @Override
  public void validate(ObjectStorageReview review) throws ValidationFailed {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      var objectStorage = review.getRequest().getObject();

      if (holder.isDefaultCustomResource(objectStorage)) {
        return;
      }

      BackupStorage spec = objectStorage.getSpec();
      String namespace = objectStorage.getMetadata().getNamespace();
      String storageType = spec.getType();

      switch (storageType) {
        case "s3" -> {
          if (spec.getS3() != null) {
            final AwsSecretKeySelector s3Selectors = spec.getS3()
                .getAwsCredentials().getSecretKeySelectors();
            var s3AccessKeySelector = s3Selectors.getAccessKeyId();
            var s3SecretKeySelector = s3Selectors.getSecretAccessKey();
            checkSecret(namespace, s3AccessKeySelector, "accessKeyId", "s3");
            checkSecret(namespace, s3SecretKeySelector, "secretAccessKey", "s3");
          }
        }
        case "s3Compatible" -> {
          if (spec.getS3Compatible() != null) {
            final AwsSecretKeySelector s3CompatibleSelectors =
                spec.getS3Compatible().getAwsCredentials().getSecretKeySelectors();
            var accessKeySelector = s3CompatibleSelectors.getAccessKeyId();
            var secretKeySelector = s3CompatibleSelectors.getSecretAccessKey();
            checkSecret(namespace, accessKeySelector, "accessKeyId", "s3Compatible");
            checkSecret(namespace, secretKeySelector, "secretAccessKey", "s3Compatible");
          }
        }
        case "azureblob" -> {
          if (spec.getAzureBlob() != null) {
            final var azureSelectors = spec.getAzureBlob()
                .getAzureCredentials().getSecretKeySelectors();
            checkSecret(namespace,
                azureSelectors.getAccessKey(),
                "accessKey", "azureblob");
            checkSecret(namespace,
                azureSelectors.getStorageAccount(),
                "account", "azureblob");
          }
        }
        case "gcs" -> {
          if (spec.getGcs() != null
              && spec.getGcs().getGcpCredentials().getSecretKeySelectors() != null) {
            final var gcsSelector = spec.getGcs().getGcpCredentials().getSecretKeySelectors();
            checkSecret(namespace,
                gcsSelector.getServiceAccountJsonKey(),
                "serviceAccountJsonKey",
                "gcs");
          }
        }
        default -> {
        }
      }

    }
  }

  private void checkSecret(String namespace,
      SecretKeySelector secretKeySelector,
      String selectorProperty,
      String storageType) throws ValidationFailed {
    var secretOpt = secretFinder.findByNameAndNamespace(
        secretKeySelector.getName(),
        namespace);

    if (secretOpt.isPresent()) {
      var data = secretOpt.map(Secret::getData)
          .orElse(Map.of());
      if (!data.containsKey(secretKeySelector.getKey())) {
        fail(
            errorTypeUri,
            String.format(
                MISSING_SECRET_KEY_MESSAGE_FORMAT,
                secretKeySelector.getKey(),
                secretKeySelector.getName(),
                selectorProperty,
                storageType));
      }
    } else {
      fail(
          errorTypeUri,
          String.format(
              MISSING_SECRET_MESSAGE_FORMAT,
              secretKeySelector.getName(),
              selectorProperty,
              storageType));
    }

  }
}
