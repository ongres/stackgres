/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import javax.annotation.Nonnull;

public enum DbOpsOperation {

  BENCHMARK("benchmark"),
  VACUUM("vacuum"),
  REPACK("repack"),
  RESTART("restart"),
  MAJOR_VERSION_UPGRADE("majorVersionUpgrade"),
  MINOR_VERSION_UPGRADE("minorVersionUpgrade"),
  SECURITY_UPGRADE("securityUpgrade");

  private final @Nonnull String type;

  DbOpsOperation(@Nonnull String type) {
    this.type = type;
  }

  @Override
  public @Nonnull String toString() {
    return type;
  }

  public static DbOpsOperation fromString(String name) {
    for (DbOpsOperation dbOps : values()) {
      if (dbOps.type.equals(name)) {
        return dbOps;
      }
    }
    throw new IllegalArgumentException("DbOps operation type is invalid: " + name);
  }

}
