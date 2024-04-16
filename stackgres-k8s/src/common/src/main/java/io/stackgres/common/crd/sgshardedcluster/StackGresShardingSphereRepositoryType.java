/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import javax.annotation.Nonnull;

public enum StackGresShardingSphereRepositoryType {

  MEMORY("Memory"),
  ZOO_KEEPER("ZooKeeper"),
  ETCD("Etcd");

  private final @Nonnull String type;

  StackGresShardingSphereRepositoryType(@Nonnull String type) {
    this.type = type;
  }

  @Override
  public @Nonnull String toString() {
    return type;
  }

  public static @Nonnull StackGresShardingSphereRepositoryType fromString(@Nonnull String value) {
    for (StackGresShardingSphereRepositoryType type : StackGresShardingSphereRepositoryType.values()) {
      if (type.toString().equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknwon sharding type " + value);
  }
}
