/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import org.jetbrains.annotations.NotNull;

public enum DbOpsSamplingMode {

  TIME("time"),
  CALLS("calls"),
  CUSTOM("custom");

  private final @NotNull String type;

  DbOpsSamplingMode(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static DbOpsSamplingMode fromString(String name) {
    for (DbOpsSamplingMode value : values()) {
      if (value.type.equals(name)) {
        return value;
      }
    }
    throw new IllegalArgumentException("sampling mode is invalid: " + name);
  }

}
