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

  public static StackGresReplicationRole fromString(String value) {
    for (StackGresReplicationRole role : StackGresReplicationRole.values()) {
      if (role.toString().equals(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Unknwon replication role " + value);
  }
}
