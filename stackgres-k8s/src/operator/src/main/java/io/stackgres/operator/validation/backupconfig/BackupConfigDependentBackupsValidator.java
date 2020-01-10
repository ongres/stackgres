/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import java.util.List;
import java.util.Optional;

import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDefinition;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;
import io.stackgres.operator.validation.BackupConfigReview;
import io.stackgres.operatorframework.Operation;
import io.stackgres.operatorframework.ValidationFailed;
import io.stackgres.operatorframework.Validator;

public class BackupConfigDependentBackupsValidator implements Validator<BackupConfigReview> {

  private final KubernetesCustomResourceScanner<StackGresBackup> backupScanner;

  public BackupConfigDependentBackupsValidator(
      KubernetesCustomResourceScanner<StackGresBackup> backupScanner) {
    this.backupScanner = backupScanner;
  }

  @Override
  public void validate(BackupConfigReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.DELETE) {
      Optional<List<StackGresBackup>> backups = backupScanner
          .findResources(review.getRequest().getNamespace());

      if (backups.isPresent()) {
        for (StackGresBackup backup : backups.get()) {
          validate(review, backup);
        }
      }
    }
  }

  private void validate(BackupConfigReview review, StackGresBackup backup) throws ValidationFailed {
    if (review.getRequest().getName().equals(backup.getStatus().getBackupConfig())) {
      fail(review, backup);
    }
  }

  private void fail(BackupConfigReview review, StackGresBackup backup) throws ValidationFailed {
    throw new ValidationFailed("Can't delete "
        + review.getRequest().getResource().getResource()
        + "." + review.getRequest().getKind().getGroup()
        + " " + review.getRequest().getName() + " because the "
        + StackGresBackupDefinition.NAME + " " + backup.getMetadata().getName() + " depends on it");
  }
}
