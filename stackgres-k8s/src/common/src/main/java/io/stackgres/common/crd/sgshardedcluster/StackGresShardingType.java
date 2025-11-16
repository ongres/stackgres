/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresShardingType {

  CITUS("citus"),
  DDP("ddp"),
  SHARDING_SPHERE("shardingsphere");

  private final @NotNull String type;

  StackGresShardingType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static @NotNull StackGresShardingType fromString(@NotNull String from) {
    for (StackGresShardingType value : StackGresShardingType.values()) {
      if (value.toString().equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknwon sharding type " + from);
  }
}
