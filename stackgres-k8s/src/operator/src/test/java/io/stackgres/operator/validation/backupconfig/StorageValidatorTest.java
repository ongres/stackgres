/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.BackupConfigReview;
import io.stackgres.operatorframework.ValidationFailed;

import org.junit.jupiter.api.Test;

class StorageValidatorTest {

  private StorageTypeValidator validator = new StorageTypeValidator();

  @Test
  void givenCreationWithoutVolumeProperties_shouldFail(){

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/invalid_creation_no_volume.json",
        BackupConfigReview.class);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String errorMessage = ex.getResult().getMessage();
    assertEquals("Invalid backup configuration,"
        + " source volume must be set when source type is volume", errorMessage);
  }

  @Test
  void givenCreationWithVolumeAndS3Properties_shouldFail(){

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/invalid_creation_volume_and_s3.json",
        BackupConfigReview.class);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String errorMessage = ex.getResult().getMessage();
    assertEquals("Invalid backup configuration,"
        + " source volume must not be set when source type is s3", errorMessage);
  }

}
