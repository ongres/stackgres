package io.stackgres.matriarch.spi;

import io.stackgres.matriarch.model.spec.DatabaseEngine;

import java.util.List;

/**
 * Resolves an engine version <em>description</em> to the exact available version, so the desired
 * spec holds a concrete version (e.g. {@code 17.4}) rather than a vague one (§5.1). Pluggable like
 * the other SPIs: a Quarkus adapter backs it with the DOCIR image catalog; the core stays
 * framework-free.
 */
public interface VersionCatalog {

    /**
     * Resolve a version description to the exact available version. A blank description resolves to
     * the latest; a major-only description like {@code 17} resolves to the latest {@code 17.x};
     * an exact {@code 17.4} resolves to itself if available. Throws if the version is unknown.
     */
    String resolveVersion(DatabaseEngine engine, String versionDescription);

    /**
     * The available version selectors for the engine — what a user may pass as {@code -v} (labels).
     */
    List<String> availableVersions(DatabaseEngine engine);

}