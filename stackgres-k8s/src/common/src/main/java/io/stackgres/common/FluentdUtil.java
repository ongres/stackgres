/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operatorframework.resource.ResourceUtil;

public class FluentdUtil {
  public static final String NAME = "fluentd";
  public static final String POSTGRES_LOG_TYPE = "postgres";
  public static final String PATRONI_LOG_TYPE = "patroni";
  public static final int FORWARD_PORT = 12225;
  public static final String FORWARD_PORT_NAME = "fluentd-fwd";

  private static final String SUFFIX = "-fluentd";
  public static final String CONFIG = "fluentd-config";
  public static final String BUFFER = "fluentd-buffer";
  public static final String LOG = "fluentd-log";

  public static String databaseNameAndOptions(StackGresCluster cluster) {
    return databaseName(cluster) + ":"
        + Optional.ofNullable(cluster.getSpec().getDistributedLogs())
        .map(StackGresClusterDistributedLogs::getRetention)
        .orElse("");
  }

  public static String databaseName(StackGresCluster cluster) {
    return databaseName(cluster.getMetadata().getNamespace(),
        cluster.getMetadata().getName());
  }

  public static String databaseName(String clusterNamespace, String clusterName) {
    return clusterNamespace + "_" + clusterName;
  }

  public static String configName(StackGresDistributedLogs distributedLogs) {
    return ResourceUtil.resourceName(distributedLogs.getMetadata().getName() + SUFFIX);
  }

  public static String serviceName(StackGresDistributedLogs distributedLogs) {
    return serviceName(distributedLogs.getMetadata().getName());
  }

  public static String serviceName(String distributedLogsName) {
    return ResourceUtil.resourceName(distributedLogsName + SUFFIX);
  }
}
