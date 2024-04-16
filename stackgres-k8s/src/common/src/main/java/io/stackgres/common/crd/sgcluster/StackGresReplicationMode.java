/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import javax.annotation.Nonnull;

public enum StackGresReplicationMode {

  ASYNC("async"),
  SYNC("sync"),
  STRICT_SYNC("strict-sync"),
  SYNC_ALL("sync-all"),
  STRICT_SYNC_ALL("strict-sync-all");

  private final @Nonnull String type;

  StackGresReplicationMode(@Nonnull String type) {
    this.type = type;
  }

  @Override
  public @Nonnull String toString() {
    return type;
  }
}
