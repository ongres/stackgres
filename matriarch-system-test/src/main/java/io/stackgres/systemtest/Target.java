package io.stackgres.systemtest;

/**
 * Which deployment the current run targets. The SAME scenarios run against each — only the config
 * (endpoint, TLS, token, environment selection) differs; the cloud just routes.
 */
public enum Target {

    /** CLI/gRPC straight to a matriarch (plaintext, no token, single implicit environment). */
    DIRECT,

    /** CLI/gRPC to the cloud (TLS, JWT, explicit/selected environment); writes route down over control.v1. */
    CLOUD;

    public boolean isCloud() {
        return this == CLOUD;
    }
}
