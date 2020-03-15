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

class ModificationValidatorTest {

  private ModificationValidator validator = new ModificationValidator(new ConfigLoader());

  @Test
  void givenModificationOfPgpConfiguration_shouldFail(){

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/invalid_modification_pgp_configuration.json",
        BackupConfigReview.class);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String errorMessage = ex.getResult().getMessage();
    assertEquals("Modification of pgp configuration is not allowed", errorMessage);
  }

  @Test
  void givenModificationOfStorage_shouldFail(){

    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/invalid_modification_storage.json",
        BackupConfigReview.class);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String errorMessage = ex.getResult().getMessage();
    assertEquals("Modification of storage is not allowed", errorMessage);
  }

}
