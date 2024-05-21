/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.common.crd.storages.AwsS3CompatibleStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AzureBlobStorage;
import io.stackgres.common.crd.storages.GoogleCloudStorage;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ObjectStorageTypeValidatorTest {

  private final ObjectStorageTypeValidator validator = new ObjectStorageTypeValidator();

  @Test
  @DisplayName("Given an invalid storage type it must fail")
  void testStorageType() {
    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();

    final String randomString = StringUtils.getRandomString(7);
    setType(review, randomString);

    assertObjectStorageReviewMessage(review,
        "Invalid storage type " + randomString
            + ", must be s3, s3Compatible, gcs or azureBlob");
  }

  @Test
  @DisplayName("Given storage type s3, s3 property must be set")
  void testStorageS3Null() {
    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();
    setType(review, "s3");

    setNullStorages(review);

    assertObjectStorageReviewMessage(review,
        "Invalid object storage. If storage type is s3, the s3 property must be set");

  }

  @Test
  @DisplayName("Given storage type s3Compatible, s3Compatible property must be set")
  void testStorageS3CompatibleNull() {

    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();
    setType(review, "s3Compatible");
    setNullStorages(review);

    assertObjectStorageReviewMessage(review, "Invalid object storage. "
        + "If storage type is s3Compatible, the s3Compatible property must be set");

  }

  @Test
  @DisplayName("Given storage type gcs, gcs property must be set")
  void testStorageGcsNull() {
    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();

    setType(review, "gcs");

    setNullStorages(review);

    assertObjectStorageReviewMessage(review, "Invalid object storage. "
        + "If storage type is gcs, the gcs property must be set");

  }

  @Test
  @DisplayName("Given storage type azureBlob, azureBlob property must be set")
  void testStorageAzureBlob() {

    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();

    setType(review, "azureBlob");

    setNullStorages(review);

    assertObjectStorageReviewMessage(review, "Invalid object storage. "
        + "If storage type is azureBlob, the azureBlob property must be set");
  }

  @Test
  @DisplayName("Given that unwanted properties set, it should fail")
  void testUnwantedPropertiesSet() {

    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();

    setType(review, "s3");
    setNullStorages(review);

    String unwantedS3Message = "Invalid object storage. "
        + "If storage type is s3, neither s3Compatible, gcs or azureBlob must be set";

    review.getRequest().getObject().getSpec().setS3(new AwsS3Storage());

    review.getRequest().getObject().getSpec().setS3Compatible(new AwsS3CompatibleStorage());
    assertObjectStorageReviewMessage(review, unwantedS3Message);

    review.getRequest().getObject().getSpec().setS3Compatible(null);
    review.getRequest().getObject().getSpec().setGcs(new GoogleCloudStorage());
    assertObjectStorageReviewMessage(review, unwantedS3Message);

    review.getRequest().getObject().getSpec().setGcs(null);
    review.getRequest().getObject().getSpec().setAzureBlob(new AzureBlobStorage());
    assertObjectStorageReviewMessage(review, unwantedS3Message);

    setType(review, "s3Compatible");
    setNullStorages(review);

    String unwantedS3CompatibleMessage = "Invalid object storage. "
        + "If storage type is s3Compatible, neither s3, gcs or azureBlob must be set";

    review.getRequest().getObject().getSpec().setS3Compatible(new AwsS3CompatibleStorage());
    review.getRequest().getObject().getSpec().setS3(new AwsS3Storage());
    assertObjectStorageReviewMessage(review, unwantedS3CompatibleMessage);

    review.getRequest().getObject().getSpec().setS3(null);
    review.getRequest().getObject().getSpec().setGcs(new GoogleCloudStorage());
    assertObjectStorageReviewMessage(review, unwantedS3CompatibleMessage);

    review.getRequest().getObject().getSpec().setGcs(null);
    review.getRequest().getObject().getSpec().setAzureBlob(new AzureBlobStorage());
    assertObjectStorageReviewMessage(review, unwantedS3CompatibleMessage);

    setType(review, "gcs");
    setNullStorages(review);

    String unwantedGcsMessage = "Invalid object storage. "
        + "If storage type is gcs, neither s3, s3Compatible or azureBlob must be set";

    review.getRequest().getObject().getSpec().setGcs(new GoogleCloudStorage());
    review.getRequest().getObject().getSpec().setS3(new AwsS3Storage());
    assertObjectStorageReviewMessage(review, unwantedGcsMessage);

    review.getRequest().getObject().getSpec().setS3(null);
    review.getRequest().getObject().getSpec().setS3Compatible(new AwsS3CompatibleStorage());
    assertObjectStorageReviewMessage(review, unwantedGcsMessage);

    review.getRequest().getObject().getSpec().setS3Compatible(null);
    review.getRequest().getObject().getSpec().setAzureBlob(new AzureBlobStorage());
    assertObjectStorageReviewMessage(review, unwantedGcsMessage);

    setType(review, "azureBlob");
    setNullStorages(review);

    String unwantedAzureBlobMessage = "Invalid object storage. "
        + "If storage type is azureBlob, neither s3, s3Compatible or gcs must be set";

    review.getRequest().getObject().getSpec().setAzureBlob(new AzureBlobStorage());
    review.getRequest().getObject().getSpec().setS3(new AwsS3Storage());
    assertObjectStorageReviewMessage(review, unwantedAzureBlobMessage);

    review.getRequest().getObject().getSpec().setS3(null);
    review.getRequest().getObject().getSpec().setS3Compatible(new AwsS3CompatibleStorage());
    assertObjectStorageReviewMessage(review, unwantedAzureBlobMessage);

    review.getRequest().getObject().getSpec().setS3Compatible(null);
    review.getRequest().getObject().getSpec().setGcs(new GoogleCloudStorage());
    assertObjectStorageReviewMessage(review, unwantedAzureBlobMessage);

  }

  @Test
  @DisplayName("Given a valid object storage creation review it must pass")
  void testValidCreation() throws ValidationFailed {

    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();

    setType(review, "s3");
    setNullStorages(review);
    review.getRequest().getObject().getSpec().setS3(new AwsS3Storage());
    validator.validate(review);

    setType(review, "s3Compatible");
    setNullStorages(review);
    review.getRequest().getObject().getSpec().setS3Compatible(new AwsS3CompatibleStorage());
    validator.validate(review);

    setType(review, "gcs");
    setNullStorages(review);
    review.getRequest().getObject().getSpec().setGcs(new GoogleCloudStorage());

    setType(review, "azureBlob");
    setNullStorages(review);
    review.getRequest().getObject().getSpec().setAzureBlob(new AzureBlobStorage());
    validator.validate(review);
  }

  @Test
  @DisplayName("Given a valid object storage update review it must pass")
  void testValidUpdate() throws ValidationFailed {

    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();

    setType(review, "s3");
    setNullStorages(review);
    review.getRequest().getObject().getSpec().setS3(new AwsS3Storage());
    validator.validate(review);

    setType(review, "s3Compatible");
    setNullStorages(review);
    review.getRequest().getObject().getSpec().setS3Compatible(new AwsS3CompatibleStorage());
    validator.validate(review);

    setType(review, "gcs");
    setNullStorages(review);
    review.getRequest().getObject().getSpec().setGcs(new GoogleCloudStorage());

    setType(review, "azureBlob");
    setNullStorages(review);
    review.getRequest().getObject().getSpec().setAzureBlob(new AzureBlobStorage());
    validator.validate(review);

  }

  @Test
  @DisplayName("Given a deletion review must pass")
  void testDelete() throws ValidationFailed {
    StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadDelete().get();
    validator.validate(review);
  }

  private void assertObjectStorageReviewMessage(
      StackGresObjectStorageReview review,
      String expectedMessage) {
    var ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));
    assertEquals(
        expectedMessage,
        ex.getMessage()
    );
  }

  private void setType(StackGresObjectStorageReview review, String storageType) {
    review.getRequest().getObject().getSpec().setType(storageType);
  }

  private void setNullStorages(StackGresObjectStorageReview review) {
    review.getRequest().getObject().getSpec().setS3(null);
    review.getRequest().getObject().getSpec().setS3Compatible(null);
    review.getRequest().getObject().getSpec().setAzureBlob(null);
    review.getRequest().getObject().getSpec().setGcs(null);
  }
}
