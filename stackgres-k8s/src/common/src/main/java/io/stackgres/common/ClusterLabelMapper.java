/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterLabelMapper implements LabelMapperForCluster {

  @Override
  public String appName() {
    return StackGresContext.CLUSTER_APP_NAME;
  }

  @Override
  public String resourceNameKey() {
    return StackGresContext.CLUSTER_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey() {
    return StackGresContext.CLUSTER_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey() {
    return StackGresContext.CLUSTER_UID_KEY;
  }

}
