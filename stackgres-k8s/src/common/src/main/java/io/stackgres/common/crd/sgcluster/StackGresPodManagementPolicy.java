/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import javax.annotation.Nonnull;

public enum StackGresPodManagementPolicy {

  ORDERED_READY("OrderedReady"),
  PARALLEL("Parallel");

  private final @Nonnull String type;

  StackGresPodManagementPolicy(@Nonnull String type) {
    this.type = type;
  }

  @Override
  public @Nonnull String toString() {
    return type;
  }
}
