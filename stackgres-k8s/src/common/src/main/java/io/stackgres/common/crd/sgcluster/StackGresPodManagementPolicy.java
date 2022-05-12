/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresPodManagementPolicy {

  ORDERED_READY("OrderedReady"),
  PARALLEL("Parallel");

  private final @NotNull String type;

  StackGresPodManagementPolicy(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }
}
