/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;

public class FluentdUtil {
  public static final String NAME = "fluentd";
  public static final String POSTGRES_LOG_TYPE = "postgres";
  public static final String PATRONI_LOG_TYPE = "patroni";
  public static final int FORWARD_PORT = 12225;
  public static final String FORWARD_PORT_NAME = "fluentd-forward";

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
}
