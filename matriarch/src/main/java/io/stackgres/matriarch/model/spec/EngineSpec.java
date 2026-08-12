package io.stackgres.matriarch.model.spec;

/** Engine-specific cluster spec (sealed). Postgres/Ivory → {@link PostgresSpec}. */
public sealed interface EngineSpec permits PostgresSpec {
}
