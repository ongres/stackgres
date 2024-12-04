/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.common.crd.sgcluster.StackGresCluster;

public class FluentdUtil {

  public static String databaseName(StackGresCluster cluster) {
    return databaseName(cluster.getMetadata().getNamespace(),
        cluster.getMetadata().getName());
  }

  public static String databaseName(String clusterNamespace, String clusterName) {
    return clusterNamespace + "_" + clusterName;
  }

}
