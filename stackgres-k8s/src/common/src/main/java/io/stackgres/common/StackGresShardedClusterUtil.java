/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operatorframework.resource.ResourceUtil;

public interface StackGresShardedClusterUtil {

  String CERTIFICATE_KEY = "tls.crt";
  String PRIVATE_KEY_KEY = "tls.key";

  static String getClusterName(StackGresShardedCluster cluster, int index) {
    if (index == 0) {
      return getCoordinatorClusterName(cluster);
    }
    return getShardClusterName(cluster, index - 1);
  }

  static String getCoordinatorClusterName(StackGresShardedCluster cluster) {
    return getCoordinatorClusterName(cluster.getMetadata().getName());
  }

  static String getCoordinatorClusterName(String name) {
    return name + "-coord";
  }

  static String getShardClusterName(StackGresShardedCluster cluster, int shardIndex) {
    return getShardClusterName(cluster, String.valueOf(shardIndex));
  }

  static String getShardClusterName(StackGresShardedCluster cluster, String shardIndex) {
    return cluster.getMetadata().getName() + "-shard" + shardIndex;
  }

  static String coordinatorConfigName(StackGresShardedCluster cluster) {
    return cluster.getMetadata().getName() + "-coord";
  }

  static String coordinatorScriptName(StackGresShardedCluster cluster) {
    return cluster.getMetadata().getName() + "-coord";
  }

  static String shardsScriptName(StackGresShardedCluster cluster) {
    return cluster.getMetadata().getName() + "-shards";
  }

  static String postgresSslSecretName(StackGresShardedCluster cluster) {
    return ResourceUtil.nameIsValidDnsSubdomain(cluster.getMetadata().getName() + "-ssl");
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
