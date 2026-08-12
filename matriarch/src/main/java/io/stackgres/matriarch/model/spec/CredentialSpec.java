package io.stackgres.matriarch.model.spec;

/**
 * Desired superuser credential. The username is not secret; {@code generate}
 * asks the matriarch to mint the password and hold it by reference (§3.7) — the
 * value never lives in this spec. (The bridge to the old slony protocol reads
 * the generated value out of the StateStore to fill the legacy inline field.)
 */
public record CredentialSpec(String username, boolean generate) {
}
