/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.customresource.sgbackup.BackupPhase;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.patroni.PatroniRole;
import io.stackgres.operator.resource.BackupFinder;
import io.stackgres.operator.validation.BackupReview;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class ForbidDeletionValidator implements BackupValidator {

  private final BackupFinder backupFinder;

  @Inject
  public ForbidDeletionValidator(BackupFinder backupFinder) {
    super();
    this.backupFinder = backupFinder;
  }

  @Override
  public void validate(BackupReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case DELETE: {
        StackGresBackup backup = backupFinder.findByNameAndNamespace(review.getRequest().getName(),
            review.getRequest().getNamespace()).orElseThrow(() -> new ValidationFailed(
                "Can not retrieve backup " + review.getRequest().getNamespace()
                + "." + review.getRequest().getName()));
        if (Optional.of(backup.getStatus())
            .map(status -> !status.getPhase().equals(BackupPhase.FAILED.label()))
            .orElse(true)
            && !review.getRequest().getUserInfo().getUsername().equals(
            "system:serviceaccount:" + review.getRequest().getNamespace() + ":"
                + PatroniRole.roleName(backup.getSpec().getCluster()))) {
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
