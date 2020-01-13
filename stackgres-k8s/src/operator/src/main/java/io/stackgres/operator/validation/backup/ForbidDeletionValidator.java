/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.customresource.sgbackup.BackupPhase;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.patroni.PatroniRole;
import io.stackgres.operator.resource.BackupScanner;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class ForbidDeletionValidator implements BackupValidator {

  private final BackupScanner backupScanner;

  @Inject
  public ForbidDeletionValidator(BackupScanner backupScanner) {
    super();
    this.backupScanner = backupScanner;
  }

  @Override
  public void validate(BackupReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case DELETE: {
        Optional<StackGresBackup> backup = backupScanner.findResources(
            review.getRequest().getNamespace())
            .flatMap(backups -> backups.stream()
                .filter(b -> b.getMetadata().getName().equals(review.getRequest().getName()))
                .findFirst());
        if (backup.map(StackGresBackup::getStatus)
            .map(status -> !status.getPhase().equals(BackupPhase.FAILED.label()))
            .orElse(true)
            && backup
            .map(b -> !review.getRequest().getUserInfo().getUsername().equals(
            "system:serviceaccount:" + review.getRequest().getNamespace() + ":"
                + PatroniRole.roleName(b.getSpec().getCluster())))
            .orElse(true)) {
          throw new ValidationFailed("Deletion of backups is forbidden"
              + " use isPermanent flag and retention to control"
              + " backup deletion");
        }
        break;
      }
      default:
    }

  }

}
