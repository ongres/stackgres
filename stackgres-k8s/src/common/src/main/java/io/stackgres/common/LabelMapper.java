/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.resource.ResourceUtil;

public interface LabelMapper<T extends CustomResource> {

  default String appKey() {
    return StackGresContext.APP_KEY;
  }

  default String clusterKey() {
    return StackGresContext.CLUSTER_KEY;
  }

  default String disruptibleKey() {
    return StackGresContext.DISRUPTIBLE_KEY;
  }

  default String backupKey() {
    return StackGresContext.BACKUP_KEY;
  }

  default String scheduledBackupKey() {
    return StackGresContext.SCHEDULED_BACKUP_KEY;
  }

  default String dbOpsKey() {
    return StackGresContext.DB_OPS_KEY;
  }

  default String clusterScopeKey() {
    return ResourceUtil.labelKey(clusterNameKey());
  }

  String appName();

  String clusterNameKey();

  String clusterNamespaceKey();

  String clusterUidKey();
}
