package io.stackgres.matriarch.model.spec;

/**
 * Engine discriminator — the domain mirror of {@code stackgres.types.v1.DatabaseEngine}.
 * Postgres and IvorySQL are first-class sibling engines (both supported today);
 * IvorySQL is Postgres-wire-compatible and shares the Postgres-family spec.
 */
public enum DatabaseEngine {
    UNSPECIFIED,
    POSTGRES,
    IVORY
}
