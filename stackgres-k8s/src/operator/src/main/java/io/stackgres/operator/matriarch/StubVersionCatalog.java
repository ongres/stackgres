package io.stackgres.operator.matriarch;

import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.spi.VersionCatalog;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

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
