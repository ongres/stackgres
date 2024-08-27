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
    for (DbOpsMethodType value : values()) {
      if (value.type.equals(name)) {
        return value;
      }
    }
    throw new IllegalArgumentException("method type is invalid: " + name);
  }

}
