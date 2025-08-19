/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import org.jetbrains.annotations.NotNull;

public enum DbOpsBenchmarkType {

  PGBENCH("pgbench"),
  SAMPLING("sampling");

  private final @NotNull String type;

  DbOpsBenchmarkType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static DbOpsBenchmarkType fromString(String from) {
    for (DbOpsBenchmarkType value : values()) {
      if (value.type.equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("benchmark type is invalid: " + from);
  }

}
