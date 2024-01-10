/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresShardingType {

  CITUS("citus"),
  DDP("ddp");

  private final @NotNull String type;

  StackGresShardingType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static @NotNull StackGresShardingType fromString(@NotNull String value) {
    for (StackGresShardingType type : StackGresShardingType.values()) {
      if (type.toString().equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknwon sharding type " + value);
  }
}
