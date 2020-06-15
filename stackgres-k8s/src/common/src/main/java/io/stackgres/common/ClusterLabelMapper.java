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
    return StackGresContext.APP_NAME;
  }

  public String clusterNameKey() {
    return StackGresContext.CLUSTER_NAME_KEY;
  }

  public String clusterNamespaceKey() {
    return StackGresContext.CLUSTER_NAMESPACE_KEY;
  }

  public String clusterUidKey() {
    return StackGresContext.CLUSTER_UID_KEY;
  }

  public String clusterKey() {
    return StackGresContext.CLUSTER_KEY;
  }

  public String disruptibleKey() {
    return StackGresContext.DISRUPTIBLE_KEY;
  }

  public String backupKey() {
    return StackGresContext.BACKUP_KEY;
  }

}
