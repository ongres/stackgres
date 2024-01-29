/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresPostgresFlavor {

  VANILLA("vanilla"),
  BABELFISH("babelfish");

  private final @NotNull String type;

  StackGresPostgresFlavor(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static @NotNull StackGresPostgresFlavor fromString(@NotNull String value) {
    for (StackGresPostgresFlavor role : StackGresPostgresFlavor.values()) {
      if (role.toString().equals(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Unknown flavor " + value);
  }

}
