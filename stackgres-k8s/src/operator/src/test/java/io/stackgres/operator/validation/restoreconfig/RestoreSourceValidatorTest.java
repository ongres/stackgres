/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import io.stackgres.operator.WithRestoreReviewResources;
import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operator.utils.ValidationUtils;

import io.stackgres.operatorframework.ValidationFailed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RestoreSourceValidatorTest implements WithRestoreReviewResources {

  private RestoreSourceValidator validator = new RestoreSourceValidator();

  @Test
  void validRestoreWithStorageCreation_shouldNotFail() throws ValidationFailed {
    RestoreConfigReview creation = getCreationReview();

    validator.validate(creation);

  }

  @Test
  void validRestoreWithStackgresBackup_shouldNotFail() throws ValidationFailed {
    RestoreConfigReview creation = getCreationReview();
    creation.getRequest().getObject().getSpec().getSource().setStackgresBackup("StackgresBackup");
    creation.getRequest().getObject().getSpec().getSource().setStorage(null);
    creation.getRequest().getObject().getSpec().getSource().setBackupName(null);

    validator.validate(creation);

  }

  @Test
  void backupNameOnly_shouldFail() {

    RestoreConfigReview creation = getCreationReview();
    creation.getRequest().getObject().getSpec().getSource().setStackgresBackup(null);
    creation.getRequest().getObject().getSpec().getSource().setStorage(null);
    creation.getRequest().getObject().getSpec().getSource().setBackupName("SomeBackup");

    ValidationUtils.assertValidationFailed(() -> validator.validate(creation),
        "A stackgres backup UUID or a backup storage must be configured");

  }

  @Test
  void storageWithoutBackupName_shouldFail() {

    RestoreConfigReview creation = getCreationReview();
    creation.getRequest().getObject().getSpec().getSource().setStackgresBackup(null);
    creation.getRequest().getObject().getSpec().getSource().setBackupName(null);

    assertNotNull(creation.getRequest().getObject().getSpec().getSource().getStorage());


    ValidationUtils.assertValidationFailed(() -> validator.validate(creation),
        "If the backup storage is configured a backup name must be specified");

  }
}