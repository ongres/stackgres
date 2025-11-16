/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresAutoscalingMode {

  ALL("all"),
  HORIZONTAL("horizontal"),
  VERTICAL("vertical"),
  NONE("none");

  private final @NotNull String type;

  StackGresAutoscalingMode(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static @NotNull StackGresAutoscalingMode fromString(@NotNull String from) {
    for (StackGresAutoscalingMode value : StackGresAutoscalingMode.values()) {
      if (value.toString().equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown autoscaling mode " + from);
  }

}
