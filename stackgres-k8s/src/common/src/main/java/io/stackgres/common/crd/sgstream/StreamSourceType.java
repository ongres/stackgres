/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import org.jetbrains.annotations.NotNull;

public enum StreamSourceType {

  SGCLUSTER("SGCluster"),
  POSTGRES("Postgres");

  private final @NotNull String type;

  StreamSourceType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static StreamSourceType fromString(String from) {
    for (StreamSourceType value : values()) {
      if (value.type.equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("SGStream source type " + from + " is invalid");
  }

}
