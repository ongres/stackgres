/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.postgres.service;

import org.jetbrains.annotations.NotNull;

public enum StackGresPostgresServiceType {

  CLUSTER_IP("ClusterIP"),
  LOAD_BALANCER("LoadBalancer"),
  NODE_PORT("NodePort");

  private final @NotNull String type;

  StackGresPostgresServiceType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }
}
