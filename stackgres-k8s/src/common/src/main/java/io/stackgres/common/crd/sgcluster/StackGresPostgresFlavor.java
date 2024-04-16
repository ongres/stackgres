/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import javax.annotation.Nonnull;

public enum StackGresPostgresFlavor {

  VANILLA("vanilla"),
  BABELFISH("babelfish");

  private final @Nonnull String type;

  StackGresPostgresFlavor(@Nonnull String type) {
    this.type = type;
  }

  @Override
  public @Nonnull String toString() {
    return type;
  }

  public static @Nonnull StackGresPostgresFlavor fromString(@Nonnull String value) {
    for (StackGresPostgresFlavor role : StackGresPostgresFlavor.values()) {
      if (role.toString().equals(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Unknown flavor " + value);
  }

}
