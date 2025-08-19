/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import org.jetbrains.annotations.NotNull;

public enum DbOpsBenchmarkConnectionType {

  PRIMARY_SERVICE("primary-service"),
  REPLICAS_SERVICE("replicas-service");

  private final @NotNull String type;

  DbOpsBenchmarkConnectionType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static DbOpsBenchmarkConnectionType fromString(String from) {
    for (DbOpsBenchmarkConnectionType value : values()) {
      if (value.type.equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("benchmark connection type is invalid: " + from);
  }

}
