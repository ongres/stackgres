/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import java.util.Random;

import javax.validation.constraints.Positive;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.operator.customresource.storages.BackupStorage;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class BackupConfigConstraintValidationTest
    extends ConstraintValidationTest<BackupConfigReview> {

  @Override
  protected ConstraintValidator<BackupConfigReview> buildValidator() {
    return new BackupConfigConstraintValidation();
  }

  @Override
  protected BackupConfigReview getValidReview() {
    return JsonUtil
        .readFromJson("backupconfig_allow_request/create.json", BackupConfigReview.class);
  }

  @Override
  protected BackupConfigReview getInvalidReview() {
    final BackupConfigReview backupConfigReview = JsonUtil
        .readFromJson("backupconfig_allow_request/create.json", BackupConfigReview.class);
    backupConfigReview.getRequest().getObject().setSpec(null);
    return backupConfigReview;
  }

  @Test
  void nullSpec_shouldFail() {
    BackupConfigReview review = getInvalidReview();

    checkNotNullErrorCause(StackGresBackupConfig.class, "spec", review);

  }

  @Test
  void nullStorage_shouldFail() {
    BackupConfigReview review = getValidReview();
    review.getRequest().getObject().getSpec().setStorage(null);

    checkNotNullErrorCause(StackGresBackupConfigSpec.class, "spec.storage", review);

  }

  @Test
  void nullStorageType_shouldFail() {

    BackupConfigReview review = getValidReview();
    review.getRequest().getObject().getSpec().getStorage().setType(null);

    checkNotNullErrorCause(BackupStorage.class, "spec.storage.type", review);

  }

  @Test
  void zeroOrNegativeRetention_shouldFail() {

    int negativeRetention = new Random().nextInt(10) * -1;

    BackupConfigReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBaseBackups().setRetention(negativeRetention);

    checkErrorCause(StackGresBaseBackupConfig.class, "spec.baseBackups.retention", review, Positive.class);

  }
}