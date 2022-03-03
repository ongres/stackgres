/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.storages.AwsCredentials;
import io.stackgres.common.crd.storages.AwsS3CompatibleStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AwsSecretKeySelector;
import io.stackgres.common.crd.storages.AzureBlobSecretKeySelector;
import io.stackgres.common.crd.storages.AzureBlobStorage;
import io.stackgres.common.crd.storages.AzureBlobStorageCredentials;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.crd.storages.GoogleCloudCredentials;
import io.stackgres.common.crd.storages.GoogleCloudSecretKeySelector;
import io.stackgres.common.crd.storages.GoogleCloudStorage;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operator.validation.DefaultCustomResourceHolder;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class BackupConfigSourceValidatorTest {

  private BackupConfigStorageValidator validator;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private DefaultCustomResourceHolder<StackGresBackupConfig> holder;

  private Secret secret;

  @BeforeEach
  void setUp() {
    validator = new BackupConfigStorageValidator(secretFinder, holder);

    secret = JsonUtil.readFromJson("secret/secret.json", Secret.class);
  }

  @Test
  void givenExistentSecretsAndKeysForS3StorageCredentialsOnCreation_shouldNotFail()
      throws ValidationFailed {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accessKeyIdName = "secret1";
    String accessKeyIdKey = "key1";
    String secretAccessKeyName = "secret2";
    String secretAccessKeyKey = "key2";
    setS3Credentials(review, accessKeyIdName, accessKeyIdKey, secretAccessKeyName,
        secretAccessKeyKey);
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);
    when(secretFinder.findByNameAndNamespace(accessKeyIdName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accessKeyIdKey, ResourceUtil.encodeSecret("accessKeyId")))
            .build()));
    when(secretFinder.findByNameAndNamespace(secretAccessKeyName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(
                ImmutableMap.of(secretAccessKeyKey, ResourceUtil.encodeSecret("secretAccessKey")))
            .build()));

    validator.validate(review);

    verify(secretFinder).findByNameAndNamespace(eq(accessKeyIdName), eq(namespace));
    verify(secretFinder).findByNameAndNamespace(eq(secretAccessKeyName), eq(namespace));
  }

  @Test
  void givenADefaultCustomResource_ShouldNotFail() throws ValidationFailed {
    final BackupConfigReview review = getEmptyReview();

    when(holder.isDefaultCustomResource(review.getRequest().getObject()))
        .thenReturn(true);

    validator.validate(review);

    verify(holder).isDefaultCustomResource(any());
    verify(secretFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenNonExistentAccessKeyIdSecretForS3StorageCredentialsOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String accessKeyIdName = "secret1";
    String accessKeyIdKey = "key1";
    String secretAccessKeyName = "secret2";
    String secretAccessKeyKey = "key2";
    setS3Credentials(review, accessKeyIdName, accessKeyIdKey, secretAccessKeyName,
        secretAccessKeyKey);

    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);
    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, secret " + accessKeyIdName
            + " for accessKeyId of s3 credentials not found",
        ex.getResult().getMessage());
  }

  @Test
  void givenNonExistentSecretAccessKeySecretForS3StorageCredentialsOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accessKeyIdName = "secret1";
    String accessKeyIdKey = "key1";
    String secretAccessKeyName = "secret2";
    String secretAccessKeyKey = "key2";
    setS3Credentials(review, accessKeyIdName, accessKeyIdKey, secretAccessKeyName,
        secretAccessKeyKey);
    when(secretFinder.findByNameAndNamespace(accessKeyIdName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accessKeyIdKey, ResourceUtil.encodeSecret("accessKeyId")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, secret " + secretAccessKeyName
            + " for secretAccessKey of s3 credentials not found",
        ex.getResult().getMessage());
    verify(secretFinder).findByNameAndNamespace(eq(accessKeyIdName), eq(namespace));
  }

  @Test
  void givenNonExistentAccessKeyIdKeyForS3StorageCredentialsOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accessKeyIdName = "secret1";
    String accessKeyIdKey = "key1";
    String secretAccessKeyName = "secret2";
    String secretAccessKeyKey = "key2";
    setS3Credentials(review, accessKeyIdName, accessKeyIdKey, secretAccessKeyName,
        secretAccessKeyKey);
    when(secretFinder.findByNameAndNamespace(accessKeyIdName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accessKeyIdKey + "-wrong",
                ResourceUtil.encodeSecret("accessKeyId")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, key " + accessKeyIdKey
            + " of secret " + accessKeyIdName
            + " for accessKeyId of s3 credentials not found",
        ex.getResult().getMessage());
    verify(secretFinder).findByNameAndNamespace(eq(accessKeyIdName), eq(namespace));
  }

  @Test
  void givenNonExistentSecretAccessKeyKeyForS3StorageCredentialsKeyOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accessKeyIdName = "secret1";
    String accessKeyIdKey = "key1";
    String secretAccessKeyName = "secret2";
    String secretAccessKeyKey = "key2";
    setS3Credentials(review, accessKeyIdName, accessKeyIdKey, secretAccessKeyName,
        secretAccessKeyKey);
    when(secretFinder.findByNameAndNamespace(accessKeyIdName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accessKeyIdKey, ResourceUtil.encodeSecret("accessKeyId")))
            .build()));
    when(secretFinder.findByNameAndNamespace(secretAccessKeyName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(secretAccessKeyKey + "-wrong",
                ResourceUtil.encodeSecret("secretAccessKey")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, key " + secretAccessKeyKey
            + " of secret " + secretAccessKeyName
            + " for secretAccessKey of s3 credentials not found",
        ex.getResult().getMessage());

    verify(secretFinder).findByNameAndNamespace(eq(accessKeyIdName), eq(namespace));
    verify(secretFinder).findByNameAndNamespace(eq(secretAccessKeyName), eq(namespace));
  }

  private void setS3Credentials(final BackupConfigReview review, String accessKeyIdName,
                                String accessKeyIdKey, String secretAccessKeyName,
                                String secretAccessKeyKey) {
    BackupStorage storage = review.getRequest().getObject().getSpec().getStorage();
    storage.setType("s3");
    storage.setS3(new AwsS3Storage());
    storage.getS3().setAwsCredentials(new AwsCredentials());
    storage.getS3().getAwsCredentials().setSecretKeySelectors(new AwsSecretKeySelector());
    AwsSecretKeySelector awsSecretKeySelector =
        storage.getS3().getAwsCredentials().getSecretKeySelectors();
    awsSecretKeySelector.setAccessKeyId(new SecretKeySelector(accessKeyIdKey, accessKeyIdName));
    awsSecretKeySelector
        .setSecretAccessKey(new SecretKeySelector(secretAccessKeyKey, secretAccessKeyName));
  }

  @Test
  void givenExistentSecretsAndKeysForS3CompatibleStorageCredentialsOnCreation_shouldNotFail()
      throws ValidationFailed {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accessKeyIdName = "secret1";
    String accessKeyIdKey = "key1";
    String secretAccessKeyName = "secret2";
    String secretAccessKeyKey = "key2";
    setS3CompatibleCredentials(review, accessKeyIdName, accessKeyIdKey, secretAccessKeyName,
        secretAccessKeyKey);
    when(secretFinder.findByNameAndNamespace(accessKeyIdName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accessKeyIdKey, ResourceUtil.encodeSecret("accessKeyId")))
            .build()));
    when(secretFinder.findByNameAndNamespace(secretAccessKeyName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(
                ImmutableMap.of(secretAccessKeyKey, ResourceUtil.encodeSecret("secretAccessKey")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);

    validator.validate(review);

    verify(secretFinder).findByNameAndNamespace(eq(accessKeyIdName), eq(namespace));
    verify(secretFinder).findByNameAndNamespace(eq(secretAccessKeyName), eq(namespace));
  }

  @Test
  void givenNonExistentAccessKeyIdSecretForS3CompatibleStorageCredentialsOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String accessKeyIdName = "secret1";
    String accessKeyIdKey = "key1";
    String secretAccessKeyName = "secret2";
    String secretAccessKeyKey = "key2";
    setS3CompatibleCredentials(review, accessKeyIdName, accessKeyIdKey, secretAccessKeyName,
        secretAccessKeyKey);
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, secret " + accessKeyIdName
            + " for accessKeyId of s3Compatible credentials not found",
        ex.getResult().getMessage());
  }

  @Test
  void givenNonExistentSecretAccessKeySecretForS3CompatibleStorageCredsOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accessKeyIdName = "secret1";
    String accessKeyIdKey = "key1";
    String secretAccessKeyName = "secret2";
    String secretAccessKeyKey = "key2";
    setS3CompatibleCredentials(review, accessKeyIdName, accessKeyIdKey, secretAccessKeyName,
        secretAccessKeyKey);
    when(secretFinder.findByNameAndNamespace(accessKeyIdName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accessKeyIdKey, ResourceUtil.encodeSecret("accessKeyId")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);
    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, secret " + secretAccessKeyName
            + " for secretAccessKey of s3Compatible credentials not found",
        ex.getResult().getMessage());
    verify(secretFinder).findByNameAndNamespace(eq(accessKeyIdName), eq(namespace));
  }

  @Test
  void givenNonExistentAccessKeyIdKeyForS3CompatibleStorageCredentialsOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accessKeyIdName = "secret1";
    String accessKeyIdKey = "key1";
    String secretAccessKeyName = "secret2";
    String secretAccessKeyKey = "key2";
    setS3CompatibleCredentials(review, accessKeyIdName, accessKeyIdKey, secretAccessKeyName,
        secretAccessKeyKey);
    when(secretFinder.findByNameAndNamespace(accessKeyIdName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accessKeyIdKey + "-wrong",
                ResourceUtil.encodeSecret("accessKeyId")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, key " + accessKeyIdKey
            + " of secret " + accessKeyIdName
            + " for accessKeyId of s3Compatible credentials not found",
        ex.getResult().getMessage());
    verify(secretFinder).findByNameAndNamespace(eq(accessKeyIdName), eq(namespace));
  }

  @Test
  void givenNonExistentSecretAccessKeyKeyForS3CompatibleStorageCredsKeyOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accessKeyIdName = "secret1";
    String accessKeyIdKey = "key1";
    String secretAccessKeyName = "secret2";
    String secretAccessKeyKey = "key2";
    setS3CompatibleCredentials(review, accessKeyIdName, accessKeyIdKey, secretAccessKeyName,
        secretAccessKeyKey);
    when(secretFinder.findByNameAndNamespace(accessKeyIdName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accessKeyIdKey, ResourceUtil.encodeSecret("accessKeyId")))
            .build()));
    when(secretFinder.findByNameAndNamespace(secretAccessKeyName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(secretAccessKeyKey + "-wrong",
                ResourceUtil.encodeSecret("secretAccessKey")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, key " + secretAccessKeyKey
            + " of secret " + secretAccessKeyName
            + " for secretAccessKey of s3Compatible credentials not found",
        ex.getResult().getMessage());

    verify(secretFinder).findByNameAndNamespace(eq(accessKeyIdName), eq(namespace));
    verify(secretFinder).findByNameAndNamespace(eq(secretAccessKeyName), eq(namespace));
  }

  private void setS3CompatibleCredentials(final BackupConfigReview review, String accessKeyIdName,
                                          String accessKeyIdKey, String secretAccessKeyName,
                                          String secretAccessKeyKey) {
    BackupStorage storage = review.getRequest().getObject().getSpec().getStorage();
    storage.setType("s3Compatible");
    storage.setS3Compatible(new AwsS3CompatibleStorage());
    storage.getS3Compatible().setAwsCredentials(new AwsCredentials());
    storage.getS3Compatible().getAwsCredentials().setSecretKeySelectors(new AwsSecretKeySelector());
    AwsSecretKeySelector awsSecretKeySelector =
        storage.getS3Compatible().getAwsCredentials().getSecretKeySelectors();
    awsSecretKeySelector.setAccessKeyId(new SecretKeySelector(accessKeyIdKey, accessKeyIdName));
    awsSecretKeySelector
        .setSecretAccessKey(new SecretKeySelector(secretAccessKeyKey, secretAccessKeyName));
  }

  @Test
  void givenExistentSecretsAndKeysForAzureBlobStorageCredentialsOnCreation_shouldNotFail()
      throws ValidationFailed {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accountName = "secret1";
    String accountKey = "key1";
    String accessKeyName = "secret2";
    String accessKeyKey = "key2";
    setAzureBlobCredentials(review, accountName, accountKey, accessKeyName, accessKeyKey);
    when(secretFinder.findByNameAndNamespace(accountName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accountKey, ResourceUtil.encodeSecret("account")))
            .build()));
    when(secretFinder.findByNameAndNamespace(accessKeyName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accessKeyKey, ResourceUtil.encodeSecret("accessKey")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);

    validator.validate(review);

    verify(secretFinder).findByNameAndNamespace(eq(accountName), eq(namespace));
    verify(secretFinder).findByNameAndNamespace(eq(accessKeyName), eq(namespace));
  }

  @Test
  void givenNonExistentAccountSecretForAzureBlobStorageCredentialsOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String accountName = "secret1";
    String accountKey = "key1";
    String accessKeyName = "secret2";
    String accessKeyKey = "key2";
    setAzureBlobCredentials(review, accountName, accountKey, accessKeyName,
        accessKeyKey);
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);
    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, secret " + accountName
            + " for account of azureblob credentials not found",
        ex.getResult().getMessage());
  }

  @Test
  void givenNonExistentSecretAccountSecretForAzureBlobStorageCredentialsOnCreation_shouldFail()
      throws ValidationFailed {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accountName = "secret1";
    String accountKey = "key1";
    String accessKeyName = "secret2";
    String accessKeyKey = "key2";
    setAzureBlobCredentials(review, accountName, accountKey, accessKeyName,
        accessKeyKey);
    when(secretFinder.findByNameAndNamespace(accountName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accountKey, ResourceUtil.encodeSecret("account")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);
    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, secret " + accessKeyName
            + " for accessKey of azureblob credentials not found",
        ex.getResult().getMessage());
    verify(secretFinder).findByNameAndNamespace(eq(accountName), eq(namespace));
  }

  @Test
  void givenNonExistentAccountKeyForAzureBlobStorageCredentialsOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accountName = "secret1";
    String accountKey = "key1";
    String accessKeyName = "secret2";
    String accessKeyKey = "key2";
    setAzureBlobCredentials(review, accountName, accountKey, accessKeyName,
        accessKeyKey);
    when(secretFinder.findByNameAndNamespace(accountName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accountKey + "-wrong", ResourceUtil.encodeSecret("account")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, key " + accountKey
            + " of secret " + accountName
            + " for account of azureblob credentials not found",
        ex.getResult().getMessage());
    verify(secretFinder).findByNameAndNamespace(eq(accountName), eq(namespace));
  }

  @Test
  void givenNonExistentAccessKeyKeyForAzureBlobStorageCredentialsKeyOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String accountName = "secret1";
    String accountKey = "key1";
    String accessKeyName = "secret2";
    String accessKeyKey = "key2";
    setAzureBlobCredentials(review, accountName, accountKey, accessKeyName,
        accessKeyKey);
    when(secretFinder.findByNameAndNamespace(accountName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(accountKey, ResourceUtil.encodeSecret("account")))
            .build()));
    when(secretFinder.findByNameAndNamespace(accessKeyName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(
                ImmutableMap.of(accessKeyKey + "-wrong", ResourceUtil.encodeSecret("accessKey")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);
    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, key " + accessKeyKey
            + " of secret " + accessKeyName
            + " for accessKey of azureblob credentials not found",
        ex.getResult().getMessage());

    verify(secretFinder).findByNameAndNamespace(eq(accountName), eq(namespace));
    verify(secretFinder).findByNameAndNamespace(eq(accessKeyName), eq(namespace));
  }

  private void setAzureBlobCredentials(final BackupConfigReview review, String accountName,
                                       String accountKey, String accessKeyName,
                                       String accessKeyKey) {
    BackupStorage storage = review.getRequest().getObject().getSpec().getStorage();
    storage.setType("azureblob");
    storage.setAzureBlob(new AzureBlobStorage());
    storage.getAzureBlob().setAzureCredentials(new AzureBlobStorageCredentials());
    storage.getAzureBlob().getAzureCredentials()
        .setSecretKeySelectors(new AzureBlobSecretKeySelector());
    AzureBlobSecretKeySelector azureBlobSecretKeySelector =
        storage.getAzureBlob().getAzureCredentials().getSecretKeySelectors();
    azureBlobSecretKeySelector.setAccount(new SecretKeySelector(accountKey, accountName));
    azureBlobSecretKeySelector.setAccessKey(new SecretKeySelector(accessKeyKey, accessKeyName));
  }

  @Test
  void givenExistentSecretsAndKeysForGcsStorageCredentialsOnCreation_shouldNotFail()
      throws ValidationFailed {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String serviceAccountJsonKeyName = "secret1";
    String serviceAccountJsonKeyKey = "key1";
    setGcsCredentials(review, serviceAccountJsonKeyName, serviceAccountJsonKeyKey);
    when(secretFinder.findByNameAndNamespace(serviceAccountJsonKeyName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(serviceAccountJsonKeyKey,
                ResourceUtil.encodeSecret("serviceAccountJsonKey")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);
    validator.validate(review);

    verify(secretFinder).findByNameAndNamespace(eq(serviceAccountJsonKeyName), eq(namespace));
  }

  @Test
  void givenNonExistentServiceAccountJsonKeySecretForGcsStorageCredentialsOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String serviceAccountJsonKeyName = "secret1";
    String serviceAccountJsonKeyKey = "key1";
    setGcsCredentials(review, serviceAccountJsonKeyName, serviceAccountJsonKeyKey);
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, secret " + serviceAccountJsonKeyName
            + " for serviceAccountJsonKey of gcs credentials not found",
        ex.getResult().getMessage());
  }

  @Test
  void givenNonExistentServiceAccountJsonKeyKeyForGcsStorageCredentialsOnCreation_shouldFail() {
    final BackupConfigReview review = getEmptyReview();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    String serviceAccountJsonKeyName = "secret1";
    String serviceAccountJsonKeyKey = "key1";
    setGcsCredentials(review, serviceAccountJsonKeyName, serviceAccountJsonKeyKey);
    when(secretFinder.findByNameAndNamespace(serviceAccountJsonKeyName, namespace))
        .thenReturn(Optional.of(new SecretBuilder(secret)
            .withData(ImmutableMap.of(serviceAccountJsonKeyKey + "-wrong",
                ResourceUtil.encodeSecret("serviceAccountJsonKey")))
            .build()));
    when(holder.isDefaultCustomResource(review.getRequest().getObject())).thenReturn(false);

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.INVALID_SECRET,
        () -> validator.validate(review));

    assertEquals("Invalid backup configuration, key " + serviceAccountJsonKeyKey
            + " of secret " + serviceAccountJsonKeyName
            + " for serviceAccountJsonKey of gcs credentials not found",
        ex.getResult().getMessage());
    verify(secretFinder).findByNameAndNamespace(eq(serviceAccountJsonKeyName), eq(namespace));
  }

  private void setGcsCredentials(final BackupConfigReview review, String serviceAccountJsonKeyName,
                                 String serviceAccountJsonKeyKey) {
    BackupStorage storage = review.getRequest().getObject().getSpec().getStorage();
    storage.setType("gcs");
    storage.setGcs(new GoogleCloudStorage());
    storage.getGcs().setCredentials(new GoogleCloudCredentials());
    storage.getGcs().getCredentials().setSecretKeySelectors(new GoogleCloudSecretKeySelector());
    GoogleCloudSecretKeySelector awsSecretKeySelector =
        storage.getGcs().getCredentials().getSecretKeySelectors();
    awsSecretKeySelector.setServiceAccountJsonKey(
        new SecretKeySelector(serviceAccountJsonKeyKey, serviceAccountJsonKeyName));
  }

  private BackupConfigReview getEmptyReview() {
    BackupConfigReview review = JsonUtil
        .readFromJson("backupconfig_allow_request/create.json", BackupConfigReview.class);
    review.getRequest().getObject().getSpec().setStorage(new BackupStorage());
    return review;
  }

}
