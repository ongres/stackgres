/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresClusterUpdateStrategyType {

  ALWAYS("Always"),
  SCHEDULE("Schedule"),
  ONLY_DB_OPS("OnlyDbOps"),
  NEVER("Never");

  private final @NotNull String type;

  StackGresClusterUpdateStrategyType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static @NotNull StackGresClusterUpdateStrategyType fromString(@NotNull String from) {
    for (StackGresClusterUpdateStrategyType value : StackGresClusterUpdateStrategyType.values()) {
      if (value.toString().equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown update strategy type " + from);
  }

}
