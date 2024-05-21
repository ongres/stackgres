/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedbackup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupInformation;
import io.stackgres.operator.common.StackGresShardedBackupReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.Test;

class ShardedBackupStatusValidatorTest {

  private final ShardedBackupStatusValidator validator = new ShardedBackupStatusValidator();

  @Test
  void validCreation_shouldPass() throws ValidationFailed {
    StackGresShardedBackupReview backupReview = AdmissionReviewFixtures.shardedBackup().loadCreate().get();

    validator.validate(backupReview);
  }

  @Test
  void validUpdate_shouldPass() throws ValidationFailed {
    StackGresShardedBackupReview backupReview = AdmissionReviewFixtures.shardedBackup().loadUpdate().get();

    validator.validate(backupReview);
  }

  @Test
  void updateStatusToNull_shouldFail() {
    StackGresShardedBackupReview backupReview = AdmissionReviewFixtures.shardedBackup().loadUpdate().get();
    backupReview.getRequest().getObject().setStatus(null);

    ValidationFailed ex = assertThrows(ValidationFailed.class,
        () -> validator.validate(backupReview));

    assertEquals("Update of referenced backups is forbidden",
        ex.getResult().getMessage());
  }

  @Test
  void updateBackupNameFromNull_shouldPass() throws ValidationFailed {
    StackGresShardedBackupReview backupReview = AdmissionReviewFixtures.shardedBackup().loadUpdate().get();
    backupReview.getRequest().getOldObject().getStatus().setSgBackups(null);
    backupReview.getRequest().getObject().getStatus().setSgBackups(
        List.of("coord", "shard1", "shard2"));

    validator.validate(backupReview);
  }

  @Test
  void updateBackupNameToNull_shouldFail() {
    StackGresShardedBackupReview backupReview = AdmissionReviewFixtures.shardedBackup().loadUpdate().get();
    backupReview.getRequest().getOldObject().getStatus().setSgBackups(
        List.of("coord", "shard1", "shard2"));
    backupReview.getRequest().getObject().getStatus().setSgBackups(null);

    ValidationFailed ex = assertThrows(ValidationFailed.class,
        () -> validator.validate(backupReview));

    assertEquals("Update of referenced backups is forbidden",
        ex.getResult().getMessage());
  }

  @Test
  void updateBackupProcess_shouldPass() throws ValidationFailed {
    StackGresShardedBackupReview backupReview = AdmissionReviewFixtures.shardedBackup().loadUpdate().get();
    backupReview.getRequest().getObject().getStatus().getProcess()
        .setStatus(ShardedBackupStatus.COMPLETED.status());

    validator.validate(backupReview);
  }

  @Test
  void updateBackupInformationFromNull_shouldPass() throws ValidationFailed {
    StackGresShardedBackupReview backupReview = AdmissionReviewFixtures.shardedBackup().loadUpdate().get();
    backupReview.getRequest().getObject().getStatus()
        .setBackupInformation(new StackGresShardedBackupInformation());

    validator.validate(backupReview);
  }

  @Test
  void updateBackupInformationToNull_shouldFail() {
    StackGresShardedBackupReview backupReview = AdmissionReviewFixtures.shardedBackup().loadUpdate().get();
    backupReview.getRequest().getOldObject().getStatus()
        .setBackupInformation(new StackGresShardedBackupInformation());
    backupReview.getRequest().getObject().getStatus()
        .setBackupInformation(null);

    ValidationFailed ex = assertThrows(ValidationFailed.class,
        () -> validator.validate(backupReview));

    assertEquals("Update of backups information is forbidden",
        ex.getResult().getMessage());
  }

}
