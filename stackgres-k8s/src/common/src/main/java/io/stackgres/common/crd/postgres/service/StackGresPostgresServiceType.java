/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.postgres.service;

import javax.annotation.Nonnull;

public enum StackGresPostgresServiceType {

  CLUSTER_IP("ClusterIP"),
  LOAD_BALANCER("LoadBalancer"),
  NODE_PORT("NodePort");

  private final @Nonnull String type;

  StackGresPostgresServiceType(@Nonnull String type) {
    this.type = type;
  }

  @Override
  public @Nonnull String toString() {
    return type;
  }
}
