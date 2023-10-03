/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedbackup;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.common.ShardedBackupReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public interface ShardedBackupValidator extends Validator<ShardedBackupReview> {

  default void fail(String reason, String message) throws ValidationFailed {
    fail(HasMetadata.getKind(StackGresShardedBackup.class), reason, message);
  }

}
