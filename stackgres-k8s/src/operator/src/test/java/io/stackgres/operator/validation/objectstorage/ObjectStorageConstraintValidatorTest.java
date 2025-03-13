/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.storages.StorageEncryption;
import io.stackgres.common.crd.storages.StorageEncryptionBuilder;
import io.stackgres.common.validation.ValidEnum;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.validation.constraints.AssertTrue;
import org.junit.jupiter.api.Test;

class ObjectStorageConstraintValidatorTest extends ConstraintValidationTest<StackGresObjectStorageReview> {

  @Override
  protected AbstractConstraintValidator<StackGresObjectStorageReview> buildValidator() {
    return new ObjectStorageConstraintValidator();
  }

  @Override
  protected StackGresObjectStorageReview getValidReview() {
    return AdmissionReviewFixtures.objectStorage().loadCreate().get();
  }

  @Override
  protected StackGresObjectStorageReview getInvalidReview() {
    final StackGresObjectStorageReview review = AdmissionReviewFixtures.objectStorage().loadCreate().get();

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    StackGresObjectStorageReview review = getValidReview();
    review.getRequest().getObject().setSpec(null);

    checkNotNullErrorCause(StackGresCluster.class, "spec", review);
  }

  @Test
  void nullEncryption_shouldPass() throws ValidationFailed {
    StackGresObjectStorageReview review = getValidReview();

    validator.validate(review);
  }

  @Test
  void invalidEncryptionMethod_shouldFail() {
    StackGresObjectStorageReview review = getValidReview();
    review.getRequest().getObject().getSpec().setEncryption(
        new StorageEncryptionBuilder()
        .withMethod("test")
        .build());

    checkErrorCause(StorageEncryption.class,
        "spec.encryption.method",
        review,
        ValidEnum.class);
  }

  @Test
  void validSodiumEncryption_shouldPass() throws ValidationFailed {
    StackGresObjectStorageReview review = getValidReview();
    review.getRequest().getObject().getSpec().setEncryption(
        new StorageEncryptionBuilder()
        .withMethod("sodium")
        .withNewSodium()
        .withNewKey()
        .withName("test")
        .withKey("test")
        .endKey()
        .endSodium()
        .build());

    validator.validate(review);
  }

  @Test
  void invalidSodiumEncryption_shouldFail() {
    StackGresObjectStorageReview review = getValidReview();
    review.getRequest().getObject().getSpec().setEncryption(
        new StorageEncryptionBuilder()
        .withMethod("sodium")
        .build());

    checkErrorCause(StorageEncryption.class,
        "spec.encryption.sodium",
        "isSodiumRequired", review,
        AssertTrue.class);
  }

  @Test
  void invalidOpenpgpEncryption_shouldFail() {
    StackGresObjectStorageReview review = getValidReview();
    review.getRequest().getObject().getSpec().setEncryption(
        new StorageEncryptionBuilder()
        .withMethod("openpgp")
        .build());

    checkErrorCause(StorageEncryption.class,
        "spec.encryption.openpgp",
        "isOpenpgpRequired", review,
        AssertTrue.class);
  }

  @Test
  void nonExclusiveSodiumEncryption_shouldFail() {
    StackGresObjectStorageReview review = getValidReview();
    review.getRequest().getObject().getSpec().setEncryption(
        new StorageEncryptionBuilder()
        .withMethod("sodium")
        .withNewSodium()
        .withNewKey()
        .withName("test")
        .withKey("test")
        .endKey()
        .endSodium()
        .withNewOpenpgp()
        .endOpenpgp()
        .build());

    checkErrorCause(StorageEncryption.class,
        new String[] { "spec.encryption.sodium", "spec.encryption.openpgp" },
        "isEncryptionMethodSectionsMutuallyExclusive", review,
        AssertTrue.class);
  }

  @Test
  void nonExclusiveOpenpgpEncryption_shouldFail() {
    StackGresObjectStorageReview review = getValidReview();
    review.getRequest().getObject().getSpec().setEncryption(
        new StorageEncryptionBuilder()
        .withMethod("openpgp")
        .withNewOpenpgp()
        .withNewKey()
        .withName("test")
        .withKey("test")
        .endKey()
        .endOpenpgp()
        .withNewSodium()
        .endSodium()
        .build());

    checkErrorCause(StorageEncryption.class,
        new String[] { "spec.encryption.sodium", "spec.encryption.openpgp" },
        "isEncryptionMethodSectionsMutuallyExclusive", review,
        AssertTrue.class);
  }

}
