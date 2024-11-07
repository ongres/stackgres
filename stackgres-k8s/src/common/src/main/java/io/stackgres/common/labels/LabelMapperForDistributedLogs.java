/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelKey;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;

public interface LabelMapperForDistributedLogs
    extends LabelMapper<StackGresDistributedLogs> {

  default String clusterKey(StackGresDistributedLogs resource) {
    return getKeyPrefix(resource) + StackGresContext.CLUSTER_KEY;
  }

  default String disruptableKey(StackGresDistributedLogs resource) {
    return getKeyPrefix(resource) + StackGresContext.DISRUPTABLE_KEY;
  }

  default String scheduledBackupKey(StackGresDistributedLogs resource) {
    return getKeyPrefix(resource) + StackGresContext.SCHEDULED_BACKUP_KEY;
  }

  default String scheduledBackupJobNameKey(StackGresDistributedLogs resource) {
    return getKeyPrefix(resource) + StackGresContext.SCHEDULED_BACKUP_JOB_NAME_KEY;
  }

  default String clusterScopeKey(StackGresDistributedLogs resource) {
    return labelKey(resourceScopeKey(resource));
  }

  default String replicationInitializationBackupKey(StackGresDistributedLogs resource) {
    return getKeyPrefix(resource) + StackGresContext.RECONCILIATION_INITIALIZATION_BACKUP_KEY;
  }

  String resourceScopeKey(StackGresDistributedLogs resource);
}
