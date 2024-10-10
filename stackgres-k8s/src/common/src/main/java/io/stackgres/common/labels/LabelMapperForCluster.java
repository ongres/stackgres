/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelKey;

import io.stackgres.common.StackGresKeys;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

public interface LabelMapperForCluster
    extends LabelMapper<StackGresCluster> {

  default String defaultConfigKey(StackGresCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.CLUSTER_DEFAULT_CONFIG_KEY;
  }

  default String clusterKey(StackGresCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.CLUSTER_KEY;
  }

  default String disruptableKey(StackGresCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.DISRUPTABLE_KEY;
  }

  default String scheduledBackupKey(StackGresCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.SCHEDULED_BACKUP_KEY;
  }

  default String scheduledBackupJobNameKey(StackGresCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.SCHEDULED_BACKUP_JOB_NAME_KEY;
  }

  default String clusterScopeKey(StackGresCluster resource) {
    return labelKey(resourceScopeKey(resource));
  }

  default String replicationInitializationBackupKey(StackGresCluster resource) {
    return getKeyPrefix(resource) + StackGresKeys.RECONCILIATION_INITIALIZATION_BACKUP_KEY;
  }

  String resourceScopeKey(StackGresCluster resource);
}
