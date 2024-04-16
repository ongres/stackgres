/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import javax.annotation.Nonnull;

public enum StackGresAuthenticationType {

  JWT("jwt"),
  OIDC("oidc");

  private final @Nonnull String type;

  StackGresAuthenticationType(@Nonnull String type) {
    this.type = type;
  }

  @Override
  public @Nonnull String toString() {
    return type;
  }

  public static @Nonnull StackGresAuthenticationType fromString(@Nonnull String name) {
    return switch (name) {
      case "jwt" -> JWT;
      case "oidc" -> OIDC;
      default -> throw new IllegalArgumentException("Unknown authentication type " + name);
    };
  }

}
