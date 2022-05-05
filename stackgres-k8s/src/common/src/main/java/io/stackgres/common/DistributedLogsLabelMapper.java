/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DistributedLogsLabelMapper implements LabelMapperForCluster {

  @Override
  public String appName() {
    return StackGresContext.DISTRIBUTED_LOGS_APP_NAME;
  }

  @Override
  public String resourceNameKey() {
    return StackGresContext.DISTRIBUTED_LOGS_CLUSTER_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey() {
    return StackGresContext.DISTRIBUTED_LOGS_CLUSTER_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey() {
    return StackGresContext.DISTRIBUTED_LOGS_CLUSTER_UID_KEY;
  }

}
