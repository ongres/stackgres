package io.stackgres.matriarch.model;

/**
 * A stop targeted a cluster that is already stopped. The adapter maps it to a gRPC
 * {@code FAILED_PRECONDITION}.
 */
public class ClusterAlreadyStoppedException extends MatriarchException {

    public ClusterAlreadyStoppedException(String name) {
        super("The cluster " + name + " is already stopped");
    }
}
