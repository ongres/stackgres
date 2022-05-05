/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.common.resource.ResourceUtil;

public interface LabelMapperForCluster extends LabelMapper {

  default String clusterKey() {
    return StackGresContext.CLUSTER_KEY;
  }

  default String disruptibleKey() {
    return StackGresContext.DISRUPTIBLE_KEY;
  }

  default String scheduledBackupKey() {
    return StackGresContext.SCHEDULED_BACKUP_KEY;
  }

  default String clusterScopeKey() {
    return ResourceUtil.labelKey(resourceNameKey());
  }

}
