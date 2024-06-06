/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedbackup;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupSpec;
import io.stackgres.operator.common.ShardedBackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedBackupConstraintValidationTest extends ConstraintValidationTest<ShardedBackupReview> {

  @Override
  protected AbstractConstraintValidator<ShardedBackupReview> buildValidator() {
    return new ShardedBackupConstraintValidation();
  }

  @Override
  protected ShardedBackupReview getValidReview() {
    return AdmissionReviewFixtures.shardedBackup().loadCreate().get();
  }

  @Override
  protected ShardedBackupReview getInvalidReview() {
    final ShardedBackupReview backupReview =
        AdmissionReviewFixtures.shardedBackup().loadCreate().get();
    backupReview.getRequest().getObject().setSpec(null);
    return backupReview;
  }

  @Test
  void nullSpec_shouldFail() {
    final ShardedBackupReview backupReview = getInvalidReview();

    checkNotNullErrorCause(StackGresShardedBackup.class, "spec", backupReview);
  }

  @Test
  void nullClusterName_shouldFail() {
    final ShardedBackupReview backupReview = getValidReview();
    backupReview.getRequest().getObject().getSpec().setSgShardedCluster(null);

    checkNotNullErrorCause(StackGresShardedBackupSpec.class, "spec.sgShardedCluster", backupReview);
  }

  @Test
  void invalidLowMaxRetries_shouldFail() {

    final ShardedBackupReview backupReview = getValidReview();
    backupReview.getRequest().getObject().getSpec().setMaxRetries(-1);

    checkErrorCause(StackGresShardedBackupSpec.class, "spec.maxRetries",
        backupReview, Min.class);

  }

}
