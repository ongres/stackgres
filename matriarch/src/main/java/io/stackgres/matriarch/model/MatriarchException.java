package io.stackgres.matriarch.model;

/**
 * Base for the matriarch's domain errors — the cluster-operation failures the adapter maps to gRPC
 * status codes at the api.v1 boundary (the core itself never throws a gRPC {@code Status}). Reserved
 * for conditions with a specific client meaning (already-exists, not-found, wrong-state); genuine
 * programming-error guards still use the JDK {@code IllegalArgumentException} /
 * {@code IllegalStateException}.
 */
public abstract class MatriarchException extends RuntimeException {

    protected MatriarchException(String message) {
        super(message);
    }
}
