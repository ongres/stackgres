/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgcluster.StackGresCluster;

@ApplicationScoped
public class ClusterLabelMapper implements LabelMapper<StackGresCluster> {

  public String appName() {
    return StackGresUtil.APP_NAME;
  }

  public String clusterNameKey() {
    return StackGresUtil.CLUSTER_NAME_KEY;
  }

  public String clusterNamespaceKey() {
    return StackGresUtil.CLUSTER_NAMESPACE_KEY;
  }

  public String clusterUidKey() {
    return StackGresUtil.CLUSTER_UID_KEY;
  }

  public String clusterKey() {
    return StackGresUtil.CLUSTER_KEY;
  }

  public String disruptibleKey() {
    return StackGresUtil.DISRUPTIBLE_KEY;
  }

  public String backupKey() {
    return StackGresUtil.BACKUP_KEY;
  }

}
