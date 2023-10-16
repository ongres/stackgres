/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardeddbops;

import org.jetbrains.annotations.NotNull;

public enum ShardedDbOpsOperation {

  RESHARDING("resharding"),
  RESTART("restart"),
  MAJOR_VERSION_UPGRADE("majorVersionUpgrade"),
  MINOR_VERSION_UPGRADE("minorVersionUpgrade"),
  SECURITY_UPGRADE("securityUpgrade");

  private final @NotNull String type;

  ShardedDbOpsOperation(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static ShardedDbOpsOperation fromString(String name) {
    for (ShardedDbOpsOperation shardedDbOps : values()) {
      if (shardedDbOps.type.equals(name)) {
        return shardedDbOps;
      }
    }
    throw new IllegalArgumentException("ShardedDbOps operation type is invalid: " + name);
  }

}
