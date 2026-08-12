package io.stackgres.matriarch.model;

/**
 * An operation targeted a cluster id that has no desired state in this environment. The adapter maps
 * it to a gRPC {@code NOT_FOUND}. Delete is exempt: deleting an absent cluster is an idempotent
 * no-op, not an error.
 */
public class ClusterNotFoundException extends MatriarchException {

    public ClusterNotFoundException(ClusterId id) {
        super("The cluster with ID " + id.value() + " doesn't exist");
    }
}