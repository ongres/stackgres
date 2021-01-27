/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

public enum StackGresClusterPostgresServiceType {

  CLUSTER_IP("ClusterIP"),
  LOAD_BALANCER("LoadBalancer"),
  NODE_PORT("NodePort");

  private final String type;

  StackGresClusterPostgresServiceType(String type) {
    this.type = type;
  }

  public String type() {
    return type;
  }
}
