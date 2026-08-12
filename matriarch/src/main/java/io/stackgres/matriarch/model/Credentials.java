package io.stackgres.matriarch.model;

/**
 * A cluster's resolved superuser credential — the username plus the plaintext password read back
 * out of the store (§3.7). Returned by {@code GetClusterCredentials}; never part of a desired spec.
 */
public record Credentials(String username, String password) {
}
