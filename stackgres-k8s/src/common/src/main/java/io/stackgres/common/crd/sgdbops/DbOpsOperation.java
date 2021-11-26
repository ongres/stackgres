/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import org.jetbrains.annotations.NotNull;

public enum DbOpsOperation {

  BENCHMARK("benchmark"),
  VACUUM("vacuum"),
  REPACK("repack"),
  RESTART("restart"),
  MAJOR_VERSION_UPGRADE("majorVersionUpgrade"),
  MINOR_VERSION_UPGRADE("minorVersionUpgrade"),
  SECURITY_UPGRADE("securityUpgrade");

  private final @NotNull String type;

  DbOpsOperation(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static DbOpsOperation fromString(String name) {
    switch (name) {
      case "benchmark":
        return BENCHMARK;
      case "vacuum":
        return VACUUM;
      case "repack":
        return REPACK;
      case "restart":
        return RESTART;
      case "majorVersionUpgrade":
        return MAJOR_VERSION_UPGRADE;
      case "minorVersionUpgrade":
        return MINOR_VERSION_UPGRADE;
      case "securityUpgrade":
        return SECURITY_UPGRADE;
      default:
        throw new IllegalArgumentException("DbOps operation type is invalid: " + name);
    }
  }

}
