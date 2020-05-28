/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.resource.ResourceUtil;

public interface LabelMapper<T extends CustomResource> {

  default String appKey() {
    return StackGresUtil.APP_KEY;
  }

  default String clusterKey() {
    return StackGresUtil.CLUSTER_KEY;
  }

  default String disruptibleKey() {
    return StackGresUtil.DISRUPTIBLE_KEY;
  }

  default String backupKey() {
    return StackGresUtil.BACKUP_KEY;
  }

  default String clusterScopeKey() {
    return ResourceUtil.labelKey(clusterNameKey());
  }

  String appName();

  String clusterNameKey();

  String clusterNamespaceKey();

  String clusterUidKey();
}
