package io.stackgres.cli.config;

/**
 * One named connection target in {@code ~/.stackgres/config.yaml}: where to reach a matriarch (local
 * or cloud), how to authenticate, and which environment to target by default. Null fields are absent
 * from the file and fall through to env vars / defaults during resolution (see
 * {@code io.stackgres.cli.CliContext#resolve()}). {@code tls} is a {@link Boolean} so "unset" (auto:
 * TLS unless localhost) is distinct from an explicit {@code false}.
 */
public record Context(String name, String endpoint, Boolean tls, String token, String environment) {
}
