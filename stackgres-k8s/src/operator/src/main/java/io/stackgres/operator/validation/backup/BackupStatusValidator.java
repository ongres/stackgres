/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupInformation;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class BackupStatusValidator implements BackupValidator {

  private String errorTypeUri;

  @Inject
  public BackupStatusValidator() {
    errorTypeUri = ErrorType.getErrorTypeUri(ErrorType.FORBIDDEN_CR_UPDATE);
  }

  @Override
  public void validate(BackupReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      String backupName = Optional.ofNullable(review.getRequest().getObject())
          .map(StackGresBackup::getStatus)
          .map(StackGresBackupStatus::getInternalName)
          .orElse(null);
      String oldBackupName = Optional.ofNullable(review.getRequest().getOldObject())
          .map(StackGresBackup::getStatus)
          .map(StackGresBackupStatus::getInternalName)
          .orElse(null);
      if (oldBackupName != null && !Objects.equals(oldBackupName, backupName)) {
        final String message = "Update of backups name is forbidden";
        fail(errorTypeUri, message);
      }
      StackGresBackupConfigSpec backupConfig = Optional
          .ofNullable(review.getRequest().getObject())
          .map(StackGresBackup::getStatus)
          .map(StackGresBackupStatus::getBackupConfig)
          .orElse(null);
      StackGresBackupConfigSpec oldBackupConfig = Optional
          .ofNullable(review.getRequest().getOldObject())
          .map(StackGresBackup::getStatus)
          .map(StackGresBackupStatus::getBackupConfig)
          .orElse(null);
      if (oldBackupConfig != null && !Objects.equals(backupConfig, oldBackupConfig)) {
        final String message = "Update of backups config is forbidden";
        fail(errorTypeUri, message);
      }
      StackGresBackupInformation backupInformation = Optional
          .ofNullable(review.getRequest().getObject())
          .map(StackGresBackup::getStatus)
          .map(StackGresBackupStatus::getBackupInformation)
          .orElse(null);
      StackGresBackupInformation oldBackupInformation = Optional
          .ofNullable(review.getRequest().getOldObject())
          .map(StackGresBackup::getStatus)
          .map(StackGresBackupStatus::getBackupInformation)
          .orElse(null);
      if (oldBackupInformation != null
          && !Objects.equals(backupInformation, oldBackupInformation)) {
        final String message = "Update of backups information is forbidden";
        fail(errorTypeUri, message);
      }
    }

  }

}
