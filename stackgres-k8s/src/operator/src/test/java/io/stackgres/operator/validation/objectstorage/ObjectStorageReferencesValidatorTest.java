/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.storages.AwsS3CompatibleStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AwsSecretKeySelector;
import io.stackgres.common.crd.storages.AzureBlobStorage;
import io.stackgres.common.crd.storages.GoogleCloudStorage;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.DefaultCustomResourceHolder;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.RandomObjectUtils;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectStorageReferencesValidatorTest {

  @Mock
  SecretFinder secretFinder;

  @Mock
  DefaultCustomResourceHolder<StackGresObjectStorage> holder;

  private ObjectStorageReferencesValidator validator;

  private static ObjectStorageReview getValidS3CreationReview() {
    ObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();

    setNullStorages(review);

    var objectStorage = review.getRequest().getObject().getSpec();
    objectStorage.setType("s3");

    final AwsS3Storage s3 = RandomObjectUtils.generateRandomObject(AwsS3Storage.class);
    objectStorage.setS3(s3);

    return review;
  }

  private static ObjectStorageReview getValidS3CompatibleCreationReview() {
    ObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();

    setNullStorages(review);

    var objectStorage = review.getRequest().getObject().getSpec();
    objectStorage.setType("s3Compatible");

    final AwsS3CompatibleStorage storage = RandomObjectUtils
        .generateRandomObject(AwsS3CompatibleStorage.class);
    objectStorage.setS3Compatible(storage);

    return review;

  }

  private static ObjectStorageReview getValidAzureBlobCreationReview() {
    ObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();

    setNullStorages(review);

    var objectStorage = review.getRequest().getObject().getSpec();
    objectStorage.setType("azureblob");

    objectStorage.setAzureBlob(
        RandomObjectUtils.generateRandomObject(AzureBlobStorage.class)
    );

    return review;

  }

  private static ObjectStorageReview getValidGcsCreationReview() {
    ObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();
    setNullStorages(review);

    var objectStorage = review.getRequest().getObject().getSpec();
    objectStorage.setType("gcs");

    objectStorage.setGcs(
        RandomObjectUtils.generateRandomObject(GoogleCloudStorage.class)
    );

    return review;

  }

  private static void setNullStorages(ObjectStorageReview review) {
    var objectStorage = review.getRequest().getObject().getSpec();
    objectStorage.setS3(null);
    objectStorage.setS3Compatible(null);
    objectStorage.setAzureBlob(null);
    objectStorage.setGcs(null);
  }

  @BeforeEach
  void setUp() {
    validator = new ObjectStorageReferencesValidator(secretFinder, holder);
  }

  @Test
  @DisplayName("Given a valid s3 object storage it should validate the referenced secret")
  void testS3References() throws ValidationFailed {

    var review = getValidS3CreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final AwsSecretKeySelector secretKeySelectors = target.getSpec().getS3().getAwsCredentials()
        .getSecretKeySelectors();

    var secretAccessKey = secretKeySelectors
        .getSecretAccessKey();
    var accessId = secretKeySelectors.getAccessKeyId();

    mockSecretFind(secretAccessKey, namespace);
    mockSecretFind(accessId, namespace);

    when(holder.isDefaultCustomResource(target)).thenReturn(false);

    validator.validate(review);

    verifySecretInteraction(secretAccessKey, namespace);
    verifySecretInteraction(accessId, namespace);
    verify(holder).isDefaultCustomResource(any());
  }

  @Test
  @DisplayName("Given a default s3 object storage it should not validate its references")
  void testS3DefaultCustomResource() throws ValidationFailed {

    var review = getValidS3CreationReview();
    var target = review.getRequest().getObject();

    when(holder.isDefaultCustomResource(target))
        .thenReturn(true);

    validator.validate(review);

    verify(holder).isDefaultCustomResource(any());

  }

  @Test
  @DisplayName("Given a s3 storage with a missing secret in the secretAccessKey must fail")
  void testS3MissingSecretAccessKeyReference() {

    var review = getValidS3CreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final AwsSecretKeySelector secretKeySelectors = target.getSpec().getS3().getAwsCredentials()
        .getSecretKeySelectors();

    var secretAccessKey = secretKeySelectors
        .getSecretAccessKey();
    var accessId = secretKeySelectors.getAccessKeyId();

    when(secretFinder.findByNameAndNamespace(secretAccessKey.getName(), namespace))
        .thenReturn(Optional.empty());
    mockSecretFind(accessId, namespace);
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, secret "
            + secretAccessKey.getName()
            + " not found for selector name secretAccessKey of storage "
            + "type s3", ex.getMessage());

  }

  @Test
  @DisplayName("Given a s3 storage with a missing secretAccessKey property in the secret must fail")
  void testS3MissingSecretAccessKeyPropertyInSecret() {
    var review = getValidS3CreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final AwsSecretKeySelector secretKeySelectors = target.getSpec().getS3().getAwsCredentials()
        .getSecretKeySelectors();

    var secretAccessKey = secretKeySelectors
        .getSecretAccessKey();
    var accessId = secretKeySelectors.getAccessKeyId();

    mockSecretFind(accessId, namespace);
    when(secretFinder.findByNameAndNamespace(secretAccessKey.getName(), namespace))
        .thenReturn(Optional.of(new Secret()));
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, key "
            + secretAccessKey.getKey() + " not found in secret " + secretAccessKey.getName()
            + " for selector name secretAccessKey of storage "
            + "type s3", ex.getMessage());
  }

  @Test
  @DisplayName("Given a s3 storage with a missing secret in the accessKeyId must fail")
  void testS3MissingAccessKeyReference() {
    var review = getValidS3CreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final AwsSecretKeySelector secretKeySelectors = target.getSpec().getS3().getAwsCredentials()
        .getSecretKeySelectors();

    var secretAccessKey = secretKeySelectors
        .getSecretAccessKey();
    var accessId = secretKeySelectors.getAccessKeyId();

    mockSecretFind(secretAccessKey, namespace);
    when(secretFinder.findByNameAndNamespace(accessId.getName(), namespace))
        .thenReturn(Optional.empty());
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, secret "
            + accessId.getName()
            + " not found for selector name accessKeyId of storage "
            + "type s3", ex.getMessage());

  }

  @Test
  @DisplayName("Given a s3 storage with a missing accessKeyId property in the secret must fail")
  void testS3MissingAccessKeyPropertyInSecret() {
    var review = getValidS3CreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final AwsSecretKeySelector secretKeySelectors = target.getSpec().getS3().getAwsCredentials()
        .getSecretKeySelectors();

    var secretAccessKey = secretKeySelectors
        .getSecretAccessKey();
    var accessId = secretKeySelectors.getAccessKeyId();

    mockSecretFind(secretAccessKey, namespace);
    when(secretFinder.findByNameAndNamespace(accessId.getName(), namespace))
        .thenReturn(Optional.of(new Secret()));
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, key "
            + accessId.getKey() + " not found in secret " + accessId.getName()
            + " for selector name accessKeyId of storage "
            + "type s3", ex.getMessage());
  }

  @Test
  @DisplayName("Given a valid s3Compatible object storage it should validate the referenced secret")
  void testS3CompatibleReferences() throws ValidationFailed {

    var review = getValidS3CompatibleCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final AwsSecretKeySelector secretKeySelectors = target.getSpec().getS3Compatible()
        .getAwsCredentials().getSecretKeySelectors();

    var secretAccessKey = secretKeySelectors
        .getSecretAccessKey();
    var accessId = secretKeySelectors.getAccessKeyId();

    mockSecretFind(secretAccessKey, namespace);
    mockSecretFind(accessId, namespace);
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    validator.validate(review);

    verifySecretInteraction(secretAccessKey, namespace);
    verifySecretInteraction(accessId, namespace);

  }

  @Test
  @DisplayName("Given a s3Compatible storage with a missing secret in the secretAccessKey "
      + "must fail")
  void testS3CompatibleMissingSecretAccessKeyReference() {

    var review = getValidS3CompatibleCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final AwsSecretKeySelector secretKeySelectors = target.getSpec().getS3Compatible()
        .getAwsCredentials()
        .getSecretKeySelectors();

    var secretAccessKey = secretKeySelectors
        .getSecretAccessKey();
    var accessId = secretKeySelectors.getAccessKeyId();

    when(secretFinder.findByNameAndNamespace(secretAccessKey.getName(), namespace))
        .thenReturn(Optional.empty());
    mockSecretFind(accessId, namespace);
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, secret "
            + secretAccessKey.getName()
            + " not found for selector name secretAccessKey of storage "
            + "type s3Compatible", ex.getMessage());

  }

  @Test
  @DisplayName("Given a s3Compatible storage with a missing secretAccessKey property in the secret "
      + "must fail")
  void testS3CompatibleMissingSecretAccessKeyPropertyInSecret() {
    var review = getValidS3CompatibleCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final AwsSecretKeySelector secretKeySelectors = target.getSpec().getS3Compatible()
        .getAwsCredentials()
        .getSecretKeySelectors();

    var secretAccessKey = secretKeySelectors
        .getSecretAccessKey();
    var accessId = secretKeySelectors.getAccessKeyId();

    mockSecretFind(accessId, namespace);
    when(secretFinder.findByNameAndNamespace(secretAccessKey.getName(), namespace))
        .thenReturn(Optional.of(new Secret()));
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, key "
            + secretAccessKey.getKey() + " not found in secret " + secretAccessKey.getName()
            + " for selector name secretAccessKey of storage "
            + "type s3Compatible", ex.getMessage());
  }

  @Test
  @DisplayName("Given a s3Compatible storage with a missing secret in the accessKeyId must fail")
  void testS3CompatibleMissingAccessKeyReference() {
    var review = getValidS3CompatibleCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final AwsSecretKeySelector secretKeySelectors = target.getSpec().getS3Compatible()
        .getAwsCredentials().getSecretKeySelectors();

    var secretAccessKey = secretKeySelectors
        .getSecretAccessKey();
    var accessId = secretKeySelectors.getAccessKeyId();

    mockSecretFind(secretAccessKey, namespace);
    when(secretFinder.findByNameAndNamespace(accessId.getName(), namespace))
        .thenReturn(Optional.empty());
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, secret "
            + accessId.getName()
            + " not found for selector name accessKeyId of storage "
            + "type s3Compatible", ex.getMessage());

  }

  @Test
  @DisplayName("Given a s3Compatible storage with a missing accessKeyId property "
      + "in the secret must fail")
  void testS3CompatibleMissingAccessKeyPropertyInSecret() {
    var review = getValidS3CompatibleCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final AwsSecretKeySelector secretKeySelectors = target.getSpec().getS3Compatible()
        .getAwsCredentials().getSecretKeySelectors();

    var secretAccessKey = secretKeySelectors
        .getSecretAccessKey();
    var accessId = secretKeySelectors.getAccessKeyId();

    mockSecretFind(secretAccessKey, namespace);
    when(secretFinder.findByNameAndNamespace(accessId.getName(), namespace))
        .thenReturn(Optional.of(new Secret()));
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, key "
            + accessId.getKey() + " not found in secret " + accessId.getName()
            + " for selector name accessKeyId of storage "
            + "type s3Compatible", ex.getMessage());
  }

  @Test
  @DisplayName("Given a valid azureblob object storage it should validate the referenced secret")
  void testAzureBlobReferences() throws ValidationFailed {

    var review = getValidAzureBlobCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final var secretKeySelectors = target.getSpec().getAzureBlob()
        .getAzureCredentials()
        .getSecretKeySelectors();

    var accessKeySelector = secretKeySelectors
        .getAccessKey();
    var accountSelector = secretKeySelectors.getAccount();

    mockSecretFind(accessKeySelector, namespace);
    mockSecretFind(accountSelector, namespace);
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    validator.validate(review);

    verifySecretInteraction(accessKeySelector, namespace);
    verifySecretInteraction(accountSelector, namespace);

  }

  @Test
  @DisplayName("Given a azureblob storage with a missing secret in the accessKey must fail")
  void testAzureblobMissingAccessKeyReference() {

    var review = getValidAzureBlobCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final var secretKeySelectors = target.getSpec().getAzureBlob()
        .getAzureCredentials()
        .getSecretKeySelectors();

    var accessKeySelector = secretKeySelectors
        .getAccessKey();
    var accountSelector = secretKeySelectors.getAccount();

    when(secretFinder.findByNameAndNamespace(accessKeySelector.getName(), namespace))
        .thenReturn(Optional.empty());
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    mockSecretFind(accountSelector, namespace);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, secret "
            + accessKeySelector.getName()
            + " not found for selector name accessKey of storage "
            + "type azureblob", ex.getMessage());

  }

  @Test
  @DisplayName("Given a azureblob storage with a missing accessKey property in the secret "
      + "must fail")
  void testAzureblobMissingAccessKeyPropertyInSecret() {
    var review = getValidAzureBlobCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final var secretKeySelectors = target.getSpec().getAzureBlob()
        .getAzureCredentials()
        .getSecretKeySelectors();

    var accessKeySelector = secretKeySelectors
        .getAccessKey();
    var accountSelector = secretKeySelectors.getAccount();

    mockSecretFind(accountSelector, namespace);
    when(secretFinder.findByNameAndNamespace(accessKeySelector.getName(), namespace))
        .thenReturn(Optional.of(new Secret()));
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, key "
            + accessKeySelector.getKey() + " not found in secret " + accessKeySelector.getName()
            + " for selector name accessKey of storage "
            + "type azureblob", ex.getMessage());
  }

  @Test
  @DisplayName("Given a azureblob storage with a missing secret in the accessKey must fail")
  void testAzureblobMissingAccountReference() {
    var review = getValidAzureBlobCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final var secretKeySelectors = target.getSpec().getAzureBlob()
        .getAzureCredentials()
        .getSecretKeySelectors();

    var accessKeySelector = secretKeySelectors
        .getAccessKey();
    var accountSelector = secretKeySelectors.getAccount();

    mockSecretFind(accountSelector, namespace);
    when(secretFinder.findByNameAndNamespace(accessKeySelector.getName(), namespace))
        .thenReturn(Optional.empty());
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, secret "
            + accessKeySelector.getName()
            + " not found for selector name accessKey of storage "
            + "type azureblob", ex.getMessage());

  }

  @Test
  @DisplayName("Given a azureblob storage with a missing account property in the secret must fail")
  void testAzureBlobMissingAccountPropertyInSecret() {
    var review = getValidAzureBlobCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final var secretKeySelectors = target.getSpec().getAzureBlob()
        .getAzureCredentials()
        .getSecretKeySelectors();

    var accessKeySelector = secretKeySelectors
        .getAccessKey();
    var accountSelector = secretKeySelectors.getAccount();

    mockSecretFind(accessKeySelector, namespace);
    when(secretFinder.findByNameAndNamespace(accountSelector.getName(), namespace))
        .thenReturn(Optional.of(new Secret()));
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, key "
            + accountSelector.getKey() + " not found in secret " + accountSelector.getName()
            + " for selector name account of storage "
            + "type azureblob", ex.getMessage());
  }

  @Test
  @DisplayName("Given a valid gcs object storage it should validate the referenced secret")
  void testGcsReferences() throws ValidationFailed {

    var review = getValidGcsCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final var secretKeySelectors = target.getSpec().getGcs()
        .getCredentials()
        .getSecretKeySelectors();

    var serviceAccountSelector = secretKeySelectors
        .getServiceAccountJsonKey();

    mockSecretFind(serviceAccountSelector, namespace);
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    validator.validate(review);

    verifySecretInteraction(serviceAccountSelector, namespace);

  }

  @Test
  @DisplayName("Given a gcs storage with a missing secret in the serviceAccountJsonKey must fail")
  void testGcsMissingServiceAccountReference() {
    var review = getValidGcsCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final var secretKeySelectors = target.getSpec().getGcs()
        .getCredentials()
        .getSecretKeySelectors();

    var serviceAccountSelector = secretKeySelectors
        .getServiceAccountJsonKey();

    when(secretFinder.findByNameAndNamespace(serviceAccountSelector.getName(), namespace))
        .thenReturn(Optional.empty());
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, secret "
            + serviceAccountSelector.getName()
            + " not found for selector name serviceAccountJsonKey of storage "
            + "type gcs", ex.getMessage());

  }

  @Test
  @DisplayName("Given a gcs storage with a serviceAccount key in secret must fail")
  void testGcsMissingServiceAccountKeyInSecretReference() {
    var review = getValidGcsCreationReview();

    var target = review.getRequest().getObject();

    String namespace = target.getMetadata().getNamespace();

    final var secretKeySelectors = target.getSpec().getGcs()
        .getCredentials()
        .getSecretKeySelectors();

    var serviceAccountSelector = secretKeySelectors
        .getServiceAccountJsonKey();

    when(secretFinder.findByNameAndNamespace(serviceAccountSelector.getName(), namespace))
        .thenReturn(Optional.of(new SecretBuilder().withData(Map.of()).build()));
    when(holder.isDefaultCustomResource(target))
        .thenReturn(false);

    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals(
        "Invalid object storage, key "
            + serviceAccountSelector.getKey() + " not found in secret "
            + serviceAccountSelector.getName()
            + " for selector name serviceAccountJsonKey of storage type gcs",
        ex.getMessage());
  }

  private void verifySecretInteraction(SecretKeySelector secretAccessKey, String namespace) {
    verify(secretFinder).findByNameAndNamespace(secretAccessKey.getName(), namespace);
  }

  private void mockSecretFind(SecretKeySelector selector, String namespace) {
    lenient().when(secretFinder.findByNameAndNamespace(selector.getName(), namespace))
        .thenReturn(Optional.of(
            new SecretBuilder()
                .withData(
                    Map.of(selector.getKey(), StringUtils.getRandomString(10))
                )
                .build()
        ));
  }
}
