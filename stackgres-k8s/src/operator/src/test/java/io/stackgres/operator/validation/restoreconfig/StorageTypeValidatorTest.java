/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import io.stackgres.operator.WithRestoreReviewResources;
import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operatorframework.ValidationFailed;
import org.junit.jupiter.api.Test;

import static io.stackgres.operator.utils.ValidationUtils.assertValidationFailed;
import static io.stackgres.operator.utils.ValidationUtils.getRandomString;

class StorageTypeValidatorTest implements WithRestoreReviewResources {

  private StorageTypeValidator validator = new StorageTypeValidator();

  @Test
  void givenAValidStorageTypeOnCreation_shouldPass() throws ValidationFailed {

    RestoreConfigReview creation = getCreationReview();

    validator.validate(creation);

  }

  @Test
  void givenAnInvalidStorageTypeOnCreation_shouldFail() {

    String invalidStorage = getRandomString(4);

    RestoreConfigReview creation = getCreationReview();
    creation.getRequest().getObject().getSpec().getSource().getStorage().setType(invalidStorage);

    assertValidationFailed(() -> validator.validate(creation),
        "Invalid storage type " + invalidStorage);

  }

  @Test
  void givenAnInvalidStorageTypeOnUpdate_shouldFail() {

    String invalidStorage = getRandomString(4);

    RestoreConfigReview creation = getUpdateReview();
    creation.getRequest().getObject().getSpec().getSource().getStorage().setType(invalidStorage);

    assertValidationFailed(() -> validator.validate(creation),
        "Invalid storage type " + invalidStorage);

  }

  @Test
  void givenS3StorageTypeAndNotS3Configuration_shouldFail() {

    RestoreConfigReview creation = getCreationReview();

    creation.getRequest().getObject().getSpec().getSource().getStorage().setType("s3");
    creation.getRequest().getObject().getSpec().getSource().getStorage().setS3(null);

    assertValidationFailed(() -> validator.validate(creation),
        "If storage type is \"s3\", a S3 configuration must be provided");

  }

  @Test
  void givenAzureStorageTypeAndNotAzureConfiguration_shouldFail() {

    RestoreConfigReview creation = getCreationReview();

    creation.getRequest().getObject().getSpec().getSource().getStorage().setType("azure");
    creation.getRequest().getObject().getSpec().getSource().getStorage().setAzureblob(null);

    assertValidationFailed(() -> validator.validate(creation),
        "If storage type is \"azure\", an Azure Blob Storage configuration must be provided");

  }

  @Test
  void givenGcsStorageTypeAndNotGcsConfiguration_shouldFail() {

    RestoreConfigReview creation = getCreationReview();

    creation.getRequest().getObject().getSpec().getSource().getStorage().setType("gcs");
    creation.getRequest().getObject().getSpec().getSource().getStorage().setGcs(null);

    assertValidationFailed(() -> validator.validate(creation),
        "If storage type is \"gcs\", "
            + "a Google Cloud Storage configuration must be provided");

  }




}