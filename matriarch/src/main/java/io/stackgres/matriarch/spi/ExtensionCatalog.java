package io.stackgres.matriarch.spi;

import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.Extension;

import java.util.List;

/**
 * Resolves requested extensions (name + optional version) for an engine and its exact version to
 * the exact {@code (name, version, revision)} — so the desired spec is concrete, the same way
 * {@link VersionCatalog} resolves the engine version. A blank requested version selects the
 * extension's default. Throws if an extension or version is unknown/unavailable.
 */
public interface ExtensionCatalog {

    List<Extension> resolveExtensions(DatabaseEngine engine, String version, List<Extension> requested);

    /**
     * All extensions available for the engine at the given EXACT version (name/version/revision).
     */
    List<Extension> availableExtensions(DatabaseEngine engine, String version);

}