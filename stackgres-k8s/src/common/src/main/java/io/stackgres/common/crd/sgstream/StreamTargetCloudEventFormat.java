/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import org.jetbrains.annotations.NotNull;

public enum StreamTargetCloudEventFormat {

  JSON("json");

  private final @NotNull String format;

  StreamTargetCloudEventFormat(@NotNull String format) {
    this.format = format;
  }

  @Override
  public @NotNull String toString() {
    return format;
  }

  public static StreamTargetCloudEventFormat fromString(String format) {
    for (StreamTargetCloudEventFormat value : values()) {
      if (value.format.equals(format)) {
        return value;
      }
    }
    throw new IllegalArgumentException("CloudEvent format " + format + " is invalid");
  }

}
