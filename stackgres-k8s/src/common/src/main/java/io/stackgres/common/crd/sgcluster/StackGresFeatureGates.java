/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import javax.annotation.Nonnull;

public enum StackGresFeatureGates {

  BABELFISH_FLAVOR("babelfish-flavor");

  private final @Nonnull String type;

  StackGresFeatureGates(@Nonnull String type) {
    this.type = type;
  }

  @Override
  public @Nonnull String toString() {
    return type;
  }
}
