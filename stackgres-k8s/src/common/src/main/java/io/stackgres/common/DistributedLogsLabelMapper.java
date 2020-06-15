/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;

@ApplicationScoped
public class DistributedLogsLabelMapper implements LabelMapper<StackGresDistributedLogs> {

  @Override
  public String appName() {
    return StackGresContext.DISTRIBUTED_LOGS_APP_NAME;
  }

  @Override
  public String clusterNameKey() {
    return StackGresContext.DISTRIBUTED_LOGS_CLUSTER_NAME_KEY;
  }

  @Override
  public String clusterNamespaceKey() {
    return StackGresContext.DISTRIBUTED_LOGS_CLUSTER_NAMESPACE_KEY;
  }

  @Override
  public String clusterUidKey() {
    return StackGresContext.DISTRIBUTED_LOGS_CLUSTER_UID_KEY;
  }

}
