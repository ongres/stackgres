/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import javax.annotation.Nonnull;

public enum StackGresShardingType {

  CITUS("citus"),
  DDP("ddp"),
  SHARDING_SPHERE("shardingsphere");

  private final @Nonnull String type;

  StackGresShardingType(@Nonnull String type) {
    this.type = type;
  }

  @Override
  public @Nonnull String toString() {
    return type;
  }

  public static @Nonnull StackGresShardingType fromString(@Nonnull String value) {
    for (StackGresShardingType type : StackGresShardingType.values()) {
      if (type.toString().equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknwon sharding type " + value);
  }
}
