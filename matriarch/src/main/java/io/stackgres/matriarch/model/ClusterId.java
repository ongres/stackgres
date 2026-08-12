package io.stackgres.matriarch.model;

/**
 * Opaque cluster identifier. Mirrors {@code stackgres.types.v1.Id} — a string, so the
 * {@code external} executor can carry engine-native ids (e.g. an RDS instance id,
 * which is not a UUID).
 */
public record ClusterId(String value) {
}
