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

  public static @NotNull StackGresPostgresFlavor fromString(@NotNull String from) {
    for (StackGresPostgresFlavor value : StackGresPostgresFlavor.values()) {
      if (value.toString().equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown flavor " + from);
  }

}
