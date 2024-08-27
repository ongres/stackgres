/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import org.jetbrains.annotations.NotNull;

public enum DbOpsPgbenchQueryMode {

  SIMPLE("simple"),
  EXTENDED("extended"),
  PREPARED("prepared");

  private final @NotNull String type;

  DbOpsPgbenchQueryMode(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static DbOpsPgbenchQueryMode fromString(String name) {
    for (DbOpsPgbenchQueryMode value : values()) {
      if (value.type.equals(name)) {
        return value;
      }
    }
    throw new IllegalArgumentException("pgbench query mode is invalid: " + name);
  }

}
