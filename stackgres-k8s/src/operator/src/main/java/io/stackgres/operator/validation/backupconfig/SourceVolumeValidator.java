/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgbackupconfig.BackupVolume;
import io.stackgres.operator.validation.BackupConfigReview;
import io.stackgres.operatorframework.Operation;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class SourceVolumeValidator implements BackupConfigValidator {

  @Override
  public void validate(BackupConfigReview review) throws ValidationFailed {

    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {

      if (review.getRequest().getObject().getSpec()
          .getStorage().getVolume() == null) {
        return;
      }

      BackupVolume volume = review.getRequest().getObject().getSpec()
          .getStorage().getVolume();
      if (volume.getNfs() == null
          && volume.getCephfs() == null
          && volume.getGlusterfs() == null) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source volume requires any of nfs, cephfs or glusterfs to be set");
      }

      if ((volume.getNfs() != null
          && volume.getCephfs() != null)
          || (volume.getNfs() != null
          && volume.getGlusterfs() != null)
          || (volume.getCephfs() != null
          && volume.getGlusterfs() != null)) {
        throw new ValidationFailed("Invalid backup configuration,"
            + " source volume requires only one of nfs, cephfs or glusterfs to be set");
      }

    }
  }
}
