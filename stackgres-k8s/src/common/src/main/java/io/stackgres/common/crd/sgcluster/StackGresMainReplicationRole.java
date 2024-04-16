/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import javax.annotation.Nonnull;

public enum StackGresMainReplicationRole {

  HA(StackGresReplicationRole.HA),
  HA_READ(StackGresReplicationRole.HA_READ);

  private final @Nonnull StackGresReplicationRole role;

  StackGresMainReplicationRole(@Nonnull StackGresReplicationRole role) {
    this.role = role;
  }

  @Override
  public @Nonnull String toString() {
    return role.toString();
  }

  public static StackGresMainReplicationRole fromString(String value) {
    for (StackGresMainReplicationRole role : StackGresMainReplicationRole.values()) {
      if (role.toString().equals(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException(value + " can not be converted to a "
        + StackGresMainReplicationRole.class.getName());
  }

}
