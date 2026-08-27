/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import java.util.List;

import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.Extension;
import io.stackgres.matriarch.spi.ExtensionCatalog;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Stub {@link ExtensionCatalog} for read-only v1 — extension resolution is only exercised on the
 * create path, which is disabled here.
 */
@ApplicationScoped
public class StubExtensionCatalog implements ExtensionCatalog {

  @Override
  public List<Extension> resolveExtensions(DatabaseEngine engine, String version, List<Extension> requested) {
    return requested;
  }

  @Override
  public List<Extension> availableExtensions(DatabaseEngine engine, String version) {
    return List.of();
  }
}
