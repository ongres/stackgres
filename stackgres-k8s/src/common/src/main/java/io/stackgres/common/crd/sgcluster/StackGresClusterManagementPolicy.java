/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresClusterManagementPolicy {

  ORDERED_READY("OrderedReady"),
  PARALLEL("Parallel");

  private final @NotNull String type;

  StackGresClusterManagementPolicy(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }
}
