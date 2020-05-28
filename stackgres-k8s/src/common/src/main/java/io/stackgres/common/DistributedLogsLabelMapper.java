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
    return StackGresUtil.DISTRIBUTED_LOGS_APP_NAME;
  }

  @Override
  public String clusterNameKey() {
    return StackGresUtil.DISTRIBUTED_LOGS_CLUSTER_NAME_KEY;
  }

  @Override
  public String clusterNamespaceKey() {
    return StackGresUtil.DISTRIBUTED_LOGS_CLUSTER_NAMESPACE_KEY;
  }

  @Override
  public String clusterUidKey() {
    return StackGresUtil.DISTRIBUTED_LOGS_CLUSTER_UID_KEY;
  }

}
