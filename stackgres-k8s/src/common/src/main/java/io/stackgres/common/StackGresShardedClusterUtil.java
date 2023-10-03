/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operatorframework.resource.ResourceUtil;

public interface StackGresShardedClusterUtil {

  static String getCoordinatorClusterName(StackGresShardedCluster cluster) {
    return cluster.getMetadata().getName() + "-coord";
  }

  static String getShardClusterName(StackGresShardedCluster cluster, int shardIndex) {
    return cluster.getMetadata().getName() + "-shard" + shardIndex;
  }

  static String primaryCoordinatorServiceName(StackGresShardedCluster cluster) {
    return primaryCoordinatorServiceName(cluster.getMetadata().getName());
  }

  static String primaryCoordinatorServiceName(String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName);
  }

  static String anyCoordinatorServiceName(StackGresShardedCluster cluster) {
    return ResourceUtil.nameIsValidService(cluster.getMetadata().getName() + "-reads");
  }

  static String primariesShardsServiceName(StackGresShardedCluster cluster) {
    return ResourceUtil.nameIsValidService(cluster.getMetadata().getName() + "-shards");
  }

}
