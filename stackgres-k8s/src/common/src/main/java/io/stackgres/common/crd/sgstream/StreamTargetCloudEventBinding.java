/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import org.jetbrains.annotations.NotNull;

public enum StreamTargetCloudEventBinding {

  HTTP("http");

  private final @NotNull String binding;

  StreamTargetCloudEventBinding(@NotNull String binding) {
    this.binding = binding;
  }

  @Override
  public @NotNull String toString() {
    return binding;
  }

  public static StreamTargetCloudEventBinding fromString(String from) {
    for (StreamTargetCloudEventBinding value : values()) {
      if (value.binding.equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("CloudEvent format " + from + " is invalid");
  }

}
