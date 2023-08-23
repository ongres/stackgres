/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelKey;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.StackGresContext;

public interface LabelMapperForCluster<T extends CustomResource<?, ?>>
    extends LabelMapper<T> {

  default String clusterKey(T resource) {
    return getKeyPrefix(resource) + StackGresContext.CLUSTER_KEY;
  }

  default String disruptableKey(T resource) {
    return getKeyPrefix(resource) + StackGresContext.DISRUPTABLE_KEY;
  }

  default String scheduledBackupKey(T resource) {
    return getKeyPrefix(resource) + StackGresContext.SCHEDULED_BACKUP_KEY;
  }

  default String scheduledBackupJobNameKey(T resource) {
    return getKeyPrefix(resource) + StackGresContext.SCHEDULED_BACKUP_JOB_NAME_KEY;
  }

  default String clusterScopeKey(T resource) {
    return labelKey(resourceScopeKey(resource));
  }

  String resourceScopeKey(T resource);
}
