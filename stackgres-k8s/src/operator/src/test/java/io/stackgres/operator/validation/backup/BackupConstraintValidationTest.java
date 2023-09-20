/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConstraintValidationTest extends ConstraintValidationTest<BackupReview> {

  @Override
  protected AbstractConstraintValidator<BackupReview> buildValidator() {
    return new BackupConstraintValidation();
  }

  @Override
  protected BackupReview getValidReview() {
    return AdmissionReviewFixtures.backup().loadCreate().get();
  }

  @Override
  protected BackupReview getInvalidReview() {
    final BackupReview backupReview = AdmissionReviewFixtures.backup().loadCreate().get();
    backupReview.getRequest().getObject().setSpec(null);
    return backupReview;
  }

  @Test
  void nullSpec_shouldFail() {
    final BackupReview backupReview = getInvalidReview();

    checkNotNullErrorCause(StackGresBackup.class, "spec", backupReview);
  }

  @Test
  void nullClusterName_shouldFail() {
    final BackupReview backupReview = getValidReview();
    backupReview.getRequest().getObject().getSpec().setSgCluster(null);

    checkNotNullErrorCause(StackGresBackupSpec.class, "spec.sgCluster", backupReview);
  }

}
