/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import javax.annotation.Nonnull;

public enum StackGresShardingSphereModeType {

  STANDALONE("Standalone"),
  CLUSTER("Cluster");

  private final @Nonnull String type;

  StackGresShardingSphereModeType(@Nonnull String type) {
    this.type = type;
  }

  @Override
  public @Nonnull String toString() {
    return type;
  }

  public static @Nonnull StackGresShardingSphereModeType fromString(@Nonnull String value) {
    for (StackGresShardingSphereModeType type : StackGresShardingSphereModeType.values()) {
      if (type.toString().equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknwon sharding type " + value);
  }
}
