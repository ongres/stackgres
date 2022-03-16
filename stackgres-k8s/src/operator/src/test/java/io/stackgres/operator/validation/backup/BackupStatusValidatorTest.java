/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackupInformation;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;

class BackupStatusValidatorTest {

  private final BackupStatusValidator validator = new BackupStatusValidator();

  @Test
  void validCreation_shouldPass() throws ValidationFailed {
    BackupReview backupReview =
        JsonUtil.readFromJson("backup_allow_request/create.json",
            BackupReview.class);

    validator.validate(backupReview);
  }

  @Test
  void validUpdate_shouldPass() throws ValidationFailed {
    BackupReview backupReview =
        JsonUtil.readFromJson("backup_allow_request/update.json",
            BackupReview.class);

    validator.validate(backupReview);
  }

  @Test
  void updateStatusToNull_shouldFail() {
    BackupReview backupReview =
        JsonUtil.readFromJson("backup_allow_request/update.json",
            BackupReview.class);
    backupReview.getRequest().getObject().setStatus(null);

    ValidationFailed ex = assertThrows(ValidationFailed.class,
        () -> validator.validate(backupReview));

    assertEquals("Update of backups config is forbidden",
        ex.getResult().getMessage());
  }

  @Test
  void updateBackupConfigStatusToNull_shouldFail() {
    BackupReview backupReview =
        JsonUtil.readFromJson("backup_allow_request/update.json",
            BackupReview.class);
    backupReview.getRequest().getObject().getStatus().setBackupConfig(null);

    ValidationFailed ex = assertThrows(ValidationFailed.class,
        () -> validator.validate(backupReview));

    assertEquals("Update of backups config is forbidden",
        ex.getResult().getMessage());
  }

  @Test
  void updateBackupNameFromNull_shouldPass() throws ValidationFailed {
    BackupReview backupReview =
        JsonUtil.readFromJson("backup_allow_request/update.json",
            BackupReview.class);
    backupReview.getRequest().getObject().getStatus().setInternalName("test");

    validator.validate(backupReview);
  }

  @Test
  void updateBackupNameToNull_shouldFail() {
    BackupReview backupReview =
        JsonUtil.readFromJson("backup_allow_request/update.json",
            BackupReview.class);
    backupReview.getRequest().getOldObject().getStatus().setInternalName("test");
    backupReview.getRequest().getObject().getStatus().setInternalName(null);

    ValidationFailed ex = assertThrows(ValidationFailed.class,
        () -> validator.validate(backupReview));

    assertEquals("Update of backups name is forbidden",
        ex.getResult().getMessage());
  }

  @Test
  void updateBackupProcess_shouldPass() throws ValidationFailed {
    BackupReview backupReview =
        JsonUtil.readFromJson("backup_allow_request/update.json",
            BackupReview.class);
    backupReview.getRequest().getObject().getStatus().getProcess()
        .setStatus(BackupPhase.COMPLETED.label());

    validator.validate(backupReview);
  }

  @Test
  void updateBackupInformationFromNull_shouldPass() throws ValidationFailed {
    BackupReview backupReview =
        JsonUtil.readFromJson("backup_allow_request/update.json",
            BackupReview.class);
    backupReview.getRequest().getObject().getStatus()
        .setBackupInformation(new StackGresBackupInformation());

    validator.validate(backupReview);
  }

  @Test
  void updateBackupInformationToNull_shouldFail() {
    BackupReview backupReview =
        JsonUtil.readFromJson("backup_allow_request/update.json",
            BackupReview.class);
    backupReview.getRequest().getOldObject().getStatus()
        .setBackupInformation(new StackGresBackupInformation());
    backupReview.getRequest().getObject().getStatus()
        .setBackupInformation(null);

    ValidationFailed ex = assertThrows(ValidationFailed.class,
        () -> validator.validate(backupReview));

    assertEquals("Update of backups information is forbidden",
        ex.getResult().getMessage());
  }

}
