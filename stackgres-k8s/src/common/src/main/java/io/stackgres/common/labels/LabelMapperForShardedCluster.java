/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresKeys;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;

public interface LabelMapperForShardedCluster
    extends LabelMapper<StackGresShardedCluster> {

  default String defaultConfigKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.SHARDED_CLUSTER_DEFAULT_CONFIG_KEY;
  }

  default String coordinatorKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.COORDINATOR_KEY;
  }

  default String workersKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.WORKERS_KEY;
  }

  default String queryRoutersKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.QUERY_ROUTERS_KEY;
  }

  default String scheduledShardedBackupKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.SCHEDULED_SHARDED_BACKUP_KEY;
  }

  default String scheduledShardedBackupJobNameKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.SCHEDULED_SHARDED_BACKUP_JOB_NAME_KEY;
  }

}
