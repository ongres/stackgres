/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;

public enum StackGresPostgresFlavor {

  VANILLA("vanilla"),
  BABELFISH("babelfish");

  private final @NotNull String type;

  StackGresPostgresFlavor(@NotNull String type) {
    this.type = type;
  }

  @Override
  @JsonValue
  public @NotNull String toString() {
    return type;
  }

  @JsonCreator
  public static @NotNull StackGresPostgresFlavor fromString(@NotNull String name) {
    return switch (name) {
      case "vanilla" -> VANILLA;
      case "babelfish" -> BABELFISH;
      default -> throw new IllegalArgumentException("Unknown flavor " + name);
    };
  }

}
