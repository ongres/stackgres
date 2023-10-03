/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedbackup;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.common.ShardedBackupReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public interface ShardedBackupMutator
    extends Mutator<StackGresShardedBackup, ShardedBackupReview> {

}
