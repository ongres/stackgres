/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.application;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public abstract class SgApplication {

  private final @NotNull String publisher;
  private final @NotNull String name;

  protected SgApplication(@NotNull String publisher, @NotNull String name) {
    this.publisher = Objects.requireNonNull(publisher, "publisher");
    this.name = Objects.requireNonNull(name, "name");
  }

  public @NotNull String publisher() {
    return publisher;
  }

  public @NotNull String appName() {
    return name;
  }

  public abstract boolean isEnabled();

}
