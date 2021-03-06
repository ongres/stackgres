/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresClusterPostgresServiceType {

  CLUSTER_IP("ClusterIP"),
  LOAD_BALANCER("LoadBalancer"),
  NODE_PORT("NodePort"),
  EXTERNAL_NAME("ExternalName");

  private final @NotNull String type;

  StackGresClusterPostgresServiceType(@NotNull String type) {
    this.type = type;
  }

  public String type() {
    return type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }
}
