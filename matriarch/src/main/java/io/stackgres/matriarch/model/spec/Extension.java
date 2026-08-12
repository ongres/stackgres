package io.stackgres.matriarch.model.spec;

/** A resolved Postgres extension: exact name, version and platform revision (§5.1). */
public record Extension(String name, String version, String revision) {
}
