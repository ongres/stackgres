/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import javax.validation.constraints.Positive;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.crd.storages.StorageClassS3;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

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

    checkErrorCause(StackGresBaseBackupConfig.class, "spec.baseBackups.retention", review,
        Positive.class);

  }

  @ParameterizedTest
  @ValueSource(strings = {"DEMO", "STANDARDIA", "reduced_redundancy"})
  void givenStorageClass_shouldFail(String storageClass) {
    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/update.json",
        BackupConfigReview.class);
    review.getRequest().getObject().getSpec().getStorage().getS3Compatible()
        .setStorageClass(storageClass);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String errorMessage = ex.getResult().getMessage();
    assertEquals("SGBackupConfig has invalid properties. "
        + "storageClass must be one of STANDARD, STANDARD_IA or REDUCED_REDUNDANCY.",
        errorMessage);
  }

  @ParameterizedTest
  @EnumSource(value = StorageClassS3.class)
  void givenStorageClass_shouldPass(StorageClassS3 storageClass) {
    BackupConfigReview review = JsonUtil.readFromJson("backupconfig_allow_request/update.json",
        BackupConfigReview.class);
    review.getRequest().getObject().getSpec().getStorage().getS3Compatible()
        .setStorageClass(storageClass.toString());

    assertDoesNotThrow(() -> validator.validate(review));
  }

}
