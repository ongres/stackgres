/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.common.ConfigLoader;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

import org.junit.jupiter.api.Test;

class StorageValidatorTest {

  private StorageTypeValidator validator = new StorageTypeValidator(new ConfigLoader());

  @Test
  void givenCreationWithoutVolumeProperties_shouldFail() {

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/invalid_creation_no_s3.json",
        BackupConfigReview.class);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String errorMessage = ex.getResult().getMessage();
    assertEquals("Invalid backup configuration,"
        + " source s3 must be set when source type is s3", errorMessage);
  }

  @Test
  void givenCreationWithGcsAndS3Properties_shouldFail() {

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/invalid_creation_gcs_and_s3.json",
        BackupConfigReview.class);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String errorMessage = ex.getResult().getMessage();
    assertEquals("Invalid backup configuration,"
        + " source gcs must not be set when source type is s3", errorMessage);
  }

}
