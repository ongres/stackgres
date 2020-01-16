/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigSource;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class RestoreSourceValidator implements RestoreConfigValidator {

  @Override
  public void validate(RestoreConfigReview review) throws ValidationFailed {

    StackgresRestoreConfig restore = review.getRequest().getObject();
    if (restore != null) {

      StackgresRestoreConfigSource source = restore.getSpec().getSource();
      if (source.getStackgresBackup() == null) {

        if (source.getStorage() == null) {
          throw new ValidationFailed(
              "A stackgres backup UUID or a backup storage must be configured");
        }
        if (source.getStorage() != null
            && source.getBackupName() == null) {
          throw new ValidationFailed(
              "If the backup storage is configured a backup name must be specified");
        }
      }

    }

  }
}
