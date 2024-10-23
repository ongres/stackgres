/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterLabelMapper implements LabelMapperForShardedCluster {

  @Override
  public String appName() {
    return StackGresContext.SHARDED_CLUSTER_APP_NAME;
  }

  @Override
  public String resourceNameKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresContext.SHARDED_CLUSTER_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresContext.SHARDED_CLUSTER_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey(StackGresShardedCluster resource) {
    return getKeyPrefix(resource) + StackGresContext.SHARDED_CLUSTER_UID_KEY;
  }

}
