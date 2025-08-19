/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import org.jetbrains.annotations.NotNull;

public enum StackGresAuthenticationType {

  JWT("jwt"),
  OIDC("oidc");

  private final @NotNull String type;

  StackGresAuthenticationType(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static @NotNull StackGresAuthenticationType fromString(@NotNull String from) {
    return switch (from) {
      case "jwt" -> JWT;
      case "oidc" -> OIDC;
      default -> throw new IllegalArgumentException("Unknown authentication type " + from);
    };
  }

}
