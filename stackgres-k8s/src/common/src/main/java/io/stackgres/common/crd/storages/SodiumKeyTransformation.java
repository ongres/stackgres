/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import org.jetbrains.annotations.NotNull;

public enum SodiumKeyTransformation {

  BASE64("base64"),
  HEX("hex"),
  NONE("none");

  private final @NotNull String type;

  SodiumKeyTransformation(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static @NotNull SodiumKeyTransformation fromString(@NotNull String from) {
    for (SodiumKeyTransformation value : SodiumKeyTransformation.values()) {
      if (value.toString().equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown sodium key transformation " + from);
  }

}
