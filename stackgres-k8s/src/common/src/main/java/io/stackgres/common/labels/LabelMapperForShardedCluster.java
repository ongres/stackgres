/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;

public interface LabelMapperForShardedCluster
    extends LabelMapper<StackGresShardedCluster> {

  default String coordinatorKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresContext.COORDINATOR_KEY;
  }

  default String shardsKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresContext.SHARDS_KEY;
  }

  default String scheduledShardedBackupKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresContext.SCHEDULED_SHARDED_BACKUP_KEY;
  }

  default String scheduledShardedBackupJobNameKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresContext.SCHEDULED_SHARDED_BACKUP_JOB_NAME_KEY;
  }

}
