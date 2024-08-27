/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import org.jetbrains.annotations.NotNull;

public enum DbOpsPgbenchMode {

  TPCB_LIKE("tpcb-like"),
  SELECT_ONLY("select-only"),
  CUSTOM("custom"),
  REPLAY("replay");

  private final @NotNull String type;

  DbOpsPgbenchMode(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static DbOpsPgbenchMode fromString(String name) {
    for (DbOpsPgbenchMode value : values()) {
      if (value.type.equals(name)) {
        return value;
      }
    }
    throw new IllegalArgumentException("pgbench mode is invalid: " + name);
  }

}
