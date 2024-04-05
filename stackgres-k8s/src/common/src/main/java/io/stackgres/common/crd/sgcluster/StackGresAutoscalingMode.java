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

  public static @NotNull StackGresAutoscalingMode fromString(@NotNull String value) {
    for (StackGresAutoscalingMode role : StackGresAutoscalingMode.values()) {
      if (role.toString().equals(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Unknown autoscaling mode " + value);
  }

}
