package io.stackgres.matriarch.model;

/**
 * A create requested a cluster name already in use in the environment. Names are unique per
 * environment and free up once the cluster is deleted (§5.1). Distinct from idempotency: this
 * rejects a different create reusing a live name, not a retry of the same create. The adapter maps
 * it to a gRPC {@code ALREADY_EXISTS}.
 */
public class ClusterNameInUseException extends MatriarchException {

    public ClusterNameInUseException(String name) {
        super("A cluster named '" + name + "' already exists");
    }
}
