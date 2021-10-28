/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;

public interface PatroniUtil {

  String SUFFIX = "-patroni";
  String READ_WRITE_SERVICE = "-primary";
  String READ_ONLY_SERVICE = "-replicas";
  String FAILOVER_SERVICE = "-failover";
  String CONFIG_SERVICE = "-config";
  int POSTGRES_SERVICE_PORT = 5432;
  int REPLICATION_SERVICE_PORT = 5433;
  int BABELFISH_SERVICE_PORT = 1433;

  static String name(@NotNull String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName);
  }

  static String readWriteName(@NotNull String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName + READ_WRITE_SERVICE);
  }

  static String readOnlyName(@NotNull String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName + READ_ONLY_SERVICE);
  }

  static String roleName(StackGresCluster cluster) {
    return roleName(cluster.getMetadata().getName());
  }

  static String roleName(String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName + SUFFIX);
  }

}
