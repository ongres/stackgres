/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.common.StackGresBackupReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class ClusterValidator implements BackupValidator {

  @Override
  public void validate(StackGresBackupReview review) throws ValidationFailed {
    StackGresBackup backup = review.getRequest().getObject();

    switch (review.getRequest().getOperation()) {
      case UPDATE -> {
        if (!review.getRequest().getOldObject().getSpec().getSgCluster()
            .equals(backup.getSpec().getSgCluster())) {
          final String message = StackGresBackup.KIND + " sgCluster can not be updated.";
          fail(message);
        }
      }
      default -> {
      }
    }
  }

}
