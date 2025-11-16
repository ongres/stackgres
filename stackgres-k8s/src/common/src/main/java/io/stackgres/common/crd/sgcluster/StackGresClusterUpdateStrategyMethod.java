/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresClusterUpdateStrategyMethod {

  IN_PLACE("InPlace"),
  REDUCED_IPACT("ReducedImpact");

  private final @NotNull String type;

  StackGresClusterUpdateStrategyMethod(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static @NotNull StackGresClusterUpdateStrategyMethod fromString(@NotNull String from) {
    for (StackGresClusterUpdateStrategyMethod value : StackGresClusterUpdateStrategyMethod.values()) {
      if (value.toString().equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown update strategy type " + from);
  }

}
