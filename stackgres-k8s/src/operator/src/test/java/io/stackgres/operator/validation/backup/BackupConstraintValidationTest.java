/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.operator.common.StackGresBackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConstraintValidationTest extends ConstraintValidationTest<StackGresBackupReview> {

  @Override
  protected AbstractConstraintValidator<StackGresBackupReview> buildValidator() {
    return new BackupConstraintValidation();
  }

  @Override
  protected StackGresBackupReview getValidReview() {
    return AdmissionReviewFixtures.backup().loadCreate().get();
  }

  @Override
  protected StackGresBackupReview getInvalidReview() {
    final StackGresBackupReview backupReview = AdmissionReviewFixtures.backup().loadCreate().get();
    backupReview.getRequest().getObject().setSpec(null);
    return backupReview;
  }

  @Test
  void nullSpec_shouldFail() {
    final StackGresBackupReview backupReview = getInvalidReview();

    checkNotNullErrorCause(StackGresBackup.class, "spec", backupReview);
  }

  @Test
  void nullClusterName_shouldFail() {
    final StackGresBackupReview backupReview = getValidReview();
    backupReview.getRequest().getObject().getSpec().setSgCluster(null);

    checkNotNullErrorCause(StackGresBackupSpec.class, "spec.sgCluster", backupReview);
  }

  @Test
  void invalidLowMaxRetries_shouldFail() {
    final StackGresBackupReview backupReview = getValidReview();
    backupReview.getRequest().getObject().getSpec().setMaxRetries(-1);

    checkErrorCause(StackGresBackupSpec.class, "spec.maxRetries",
        backupReview, Min.class);
  }

}
