/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import org.jetbrains.annotations.NotNull;

public enum StreamTargetType {

  CLOUD_EVENT("CloudEvent"),
  PG_LAMBDA("PgLambda"),
  SGCLUSTER("SGCluster");

  private final @NotNull String type;

  StreamTargetType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static StreamTargetType fromString(String type) {
    for (StreamTargetType value : values()) {
      if (value.type.equals(type)) {
        return value;
      }
    }
    throw new IllegalArgumentException("SGStream target type " + type + " is invalid");
  }

}
