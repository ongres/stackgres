/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardeddbops;

import org.jetbrains.annotations.NotNull;

public enum ShardedDbOpsOperationAllowed {

  RESHARDING("resharding"),
  RESTART("restart"),
  SECURITY_UPGRADE("securityUpgrade");

  private final @NotNull String type;

  ShardedDbOpsOperationAllowed(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static ShardedDbOpsOperationAllowed fromString(String name) {
    for (ShardedDbOpsOperationAllowed shardedDbOps : values()) {
      if (shardedDbOps.type.equals(name)) {
        return shardedDbOps;
      }
    }
    throw new IllegalArgumentException("ShardedDbOps operation type is invalid: " + name);
  }

}
