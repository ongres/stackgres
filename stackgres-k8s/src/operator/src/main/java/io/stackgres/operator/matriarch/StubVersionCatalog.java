/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import java.util.List;

import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.spi.VersionCatalog;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Stub {@link VersionCatalog} for read-only v1 — version resolution is only exercised on the create
 * path, which is disabled here. Wired to StackGres's supported-Postgres versions in a later step.
 */
@ApplicationScoped
public class StubVersionCatalog implements VersionCatalog {

  @Override
  public String resolveVersion(DatabaseEngine engine, String versionDescription) {
    return versionDescription;
  }

  @Override
  public List<String> availableVersions(DatabaseEngine engine) {
    return List.of();
  }
}
