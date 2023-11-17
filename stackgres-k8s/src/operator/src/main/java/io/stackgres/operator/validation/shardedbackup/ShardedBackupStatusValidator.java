/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedbackup;

import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupInformation;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;
import io.stackgres.operator.common.ShardedBackupReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class ShardedBackupStatusValidator implements ShardedBackupValidator {

  private String errorTypeUri;

  @Inject
  public ShardedBackupStatusValidator() {
    errorTypeUri = ErrorType.getErrorTypeUri(ErrorType.FORBIDDEN_CR_UPDATE);
  }

  @Override
  public void validate(ShardedBackupReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      var backups = Optional
          .ofNullable(review.getRequest().getObject())
          .map(StackGresShardedBackup::getStatus)
          .map(StackGresShardedBackupStatus::getSgBackups)
          .orElse(null);
      var oldBackups = Optional
          .ofNullable(review.getRequest().getOldObject())
          .map(StackGresShardedBackup::getStatus)
          .map(StackGresShardedBackupStatus::getSgBackups)
          .orElse(null);
      if (oldBackups != null && !Objects.equals(backups, oldBackups)) {
        final String message = "Update of referenced backups is forbidden";
        fail(errorTypeUri, message);
      }
      StackGresShardedBackupInformation backupInformation = Optional
          .ofNullable(review.getRequest().getObject())
          .map(StackGresShardedBackup::getStatus)
          .map(StackGresShardedBackupStatus::getBackupInformation)
          .orElse(null);
      StackGresShardedBackupInformation oldBackupInformation = Optional
          .ofNullable(review.getRequest().getOldObject())
          .map(StackGresShardedBackup::getStatus)
          .map(StackGresShardedBackupStatus::getBackupInformation)
          .orElse(null);
      if (oldBackupInformation != null
          && !Objects.equals(backupInformation, oldBackupInformation)) {
        final String message = "Update of backups information is forbidden";
        fail(errorTypeUri, message);
      }
    }

  }

}
