/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;

public interface LabelMapperForShardedBackup
    extends LabelMapper<StackGresShardedBackup> {

  default String shardedBackupKey(StackGresShardedBackup resource) {
    return getKeyPrefix(resource) + StackGresContext.SHARDED_BACKUP_KEY;
  }

}
