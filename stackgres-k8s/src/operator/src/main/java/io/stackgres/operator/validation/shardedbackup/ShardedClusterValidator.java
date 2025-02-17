/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedbackup;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.common.StackGresShardedBackupReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class ShardedClusterValidator implements ShardedBackupValidator {

  @Override
  public void validate(StackGresShardedBackupReview review) throws ValidationFailed {
    StackGresShardedBackup backup = review.getRequest().getObject();

    switch (review.getRequest().getOperation()) {
      case UPDATE -> {
        if (!review.getRequest().getOldObject().getSpec().getSgShardedCluster()
            .equals(backup.getSpec().getSgShardedCluster())) {
          fail(StackGresShardedBackup.KIND + " sgShardedCluster can not be updated.");
        }
      }
      default -> {
      }
    }
  }

}
