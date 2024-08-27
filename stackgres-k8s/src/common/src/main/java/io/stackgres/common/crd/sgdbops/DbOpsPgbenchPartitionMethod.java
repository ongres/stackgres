/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import org.jetbrains.annotations.NotNull;

public enum DbOpsPgbenchPartitionMethod {

  RANGE("range"),
  HASH("hash");

  private final @NotNull String type;

  DbOpsPgbenchPartitionMethod(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static DbOpsPgbenchPartitionMethod fromString(String name) {
    for (DbOpsPgbenchPartitionMethod value : values()) {
      if (value.type.equals(name)) {
        return value;
      }
    }
    throw new IllegalArgumentException("pgbench partition method is invalid: " + name);
  }

}
