/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresShardingSphereRepositoryType {

  MEMORY("Memory"),
  ZOO_KEEPER("ZooKeeper"),
  ETCD("Etcd");

  private final @NotNull String type;

  StackGresShardingSphereRepositoryType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static @NotNull StackGresShardingSphereRepositoryType fromString(@NotNull String value) {
    for (StackGresShardingSphereRepositoryType type : StackGresShardingSphereRepositoryType.values()) {
      if (type.toString().equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknwon sharding type " + value);
  }
}
