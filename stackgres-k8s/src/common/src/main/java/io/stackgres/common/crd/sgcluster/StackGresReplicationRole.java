/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresReplicationRole {

  HA("ha"),
  HA_READ("ha-read"),
  READONLY("readonly"),
  NONE("none");

  private final @NotNull String type;

  StackGresReplicationRole(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static StackGresReplicationRole fromString(String from) {
    for (StackGresReplicationRole value : StackGresReplicationRole.values()) {
      if (value.toString().equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknwon replication role " + from);
  }
}
