/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.application;

import java.util.Objects;

import javax.annotation.Nonnull;

public abstract class SgApplication {

  private final @Nonnull String publisher;
  private final @Nonnull String name;

  protected SgApplication(@Nonnull String publisher, @Nonnull String name) {
    this.publisher = Objects.requireNonNull(publisher, "publisher");
    this.name = Objects.requireNonNull(name, "name");
  }

  public @Nonnull String publisher() {
    return publisher;
  }

  public @Nonnull String appName() {
    return name;
  }

  public abstract boolean isEnabled();

}
