/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import java.util.Set;

import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.spi.Executor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * v1 is read-only: the {@code StackGresObserver} feeds desired/observed state; nothing provisions.
 * The core still requires an {@link Executor}, so this one exists and rejects every mutation — a
 * belt-and-braces backstop behind the RPC-layer read-only guard.
 */
@ApplicationScoped
public class ReadOnlyExecutor implements Executor {

  @Override
  public void apply(ClusterSpec desired) {
    throw readOnly();
  }

  @Override
  public void remove(ClusterSpec spec) {
    throw readOnly();
  }

  @Override
  public void start(ClusterSpec spec) {
    throw readOnly();
  }

  @Override
  public void stop(ClusterSpec spec) {
    throw readOnly();
  }

  @Override
  public void restart(ClusterSpec spec) {
    throw readOnly();
  }

  @Override
  public Set<String> capabilities() {
    return Set.of();
  }

  private static UnsupportedOperationException readOnly() {
    return new UnsupportedOperationException("read-only environment");
  }
}
