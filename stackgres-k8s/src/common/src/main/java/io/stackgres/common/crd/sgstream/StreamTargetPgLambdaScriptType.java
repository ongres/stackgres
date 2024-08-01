/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import org.jetbrains.annotations.NotNull;

public enum StreamTargetPgLambdaScriptType {

  JAVASCRIPT("javascript");

  private final @NotNull String type;

  StreamTargetPgLambdaScriptType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static StreamTargetPgLambdaScriptType fromString(String type) {
    for (StreamTargetPgLambdaScriptType value : values()) {
      if (value.type.equals(type)) {
        return value;
      }
    }
    throw new IllegalArgumentException("PgLambda script type " + type + " is invalid");
  }

}
