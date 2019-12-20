/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.validation.BackupReview;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class BackupNameValidator implements BackupValidator {

  @Override
  public void validate(BackupReview review) throws ValidationFailed {
    String name = Optional.ofNullable(review.getRequest().getObject().getStatus())
        .map(status -> status.getName())
        .orElse(null);
    String oldName = Optional.ofNullable(review.getRequest().getOldObject().getStatus())
        .map(status -> status.getName())
        .orElse(null);

    switch (review.getRequest().getOperation()) {
      case UPDATE:
        if (oldName != null && !Objects.equals(oldName, name)) {
          throw new ValidationFailed("Update of backups name is forbidden");
        }
        break;
      default:
    }

  }

}
