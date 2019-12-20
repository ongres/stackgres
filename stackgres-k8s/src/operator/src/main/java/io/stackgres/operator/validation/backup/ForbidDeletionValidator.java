/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.validation.BackupReview;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class ForbidDeletionValidator implements BackupValidator {

  @Override
  public void validate(BackupReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case DELETE:
        throw new ValidationFailed("Deletion of backups is forbidden"
            + " user isPermanent flag and retention to control"
            + " backup deletion");
      default:
    }

  }

}
