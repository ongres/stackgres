package io.stackgres.matriarch.model;

/**
 * A start targeted a cluster that is already running (or on its way up). The adapter maps it to a
 * gRPC {@code FAILED_PRECONDITION}.
 */
public class ClusterAlreadyRunningException extends MatriarchException {

    public ClusterAlreadyRunningException(String name) {
        super("The cluster " + name + " is already running");
    }
}
