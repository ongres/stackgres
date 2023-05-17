/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresReplicationMode {

  ASYNC("async"),
  SYNC("sync"),
  STRICT_SYNC("strict-sync"),
  SYNC_ALL("sync-all"),
  STRICT_SYNC_ALL("strict-sync-all");

  private final @NotNull String type;

  StackGresReplicationMode(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }
}
