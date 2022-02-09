/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresReplicationMode {

  ASYNC("ASYNC"),
  SYNC("SYNC"),
  STRICT_SYNC("STRICT_SYNC");

  private final @NotNull String type;

  StackGresReplicationMode(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }
}
