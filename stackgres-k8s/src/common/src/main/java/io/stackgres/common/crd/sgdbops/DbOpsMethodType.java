/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import org.jetbrains.annotations.NotNull;

public enum DbOpsMethodType {

  IN_PLACE("InPlace"),
  REDUCED_IMPACT("ReducedImpact");

  private final @NotNull String type;

  DbOpsMethodType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static DbOpsMethodType fromString(String name) {
    switch (name) {
      case "InPlace":
        return IN_PLACE;
      case "ReducedImpact":
        return REDUCED_IMPACT;
      default:
        throw new IllegalArgumentException("DbOps method type is invalid: " + name);
    }
  }

}
