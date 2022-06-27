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

  public static @NotNull StackGresPostgresFlavor fromString(@NotNull String name) {
    return switch (name) {
      case "vanilla" -> VANILLA;
      case "babelfish" -> BABELFISH;
      default -> throw new IllegalArgumentException("Unknown flavor " + name);
    };
  }

}
