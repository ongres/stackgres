package io.stackgres.matriarch.model.spec;

import java.util.List;
import java.util.Map;

/**
 * Postgres-family engine spec (covers POSTGRES and IVORY). Resolved extensions
 * (exact version + revision, via the {@code ExtensionCatalog}) and postgresql.conf
 * settings; both empty for a plain standalone.
 */
public record PostgresSpec(List<Extension> extensions, Map<String, String> settings) implements EngineSpec {

    public static PostgresSpec empty() {
        return new PostgresSpec(List.of(), Map.of());
    }
}
