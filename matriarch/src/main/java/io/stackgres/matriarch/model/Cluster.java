package io.stackgres.matriarch.model;
import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.model.status.ClusterStatus;

/**
 * Read projection: desired {@link ClusterSpec} + last observed {@link ClusterStatus}.
 * The {@code api.v1.Cluster} a client sees is the merge of these two (§4.2); the
 * StateStore persists only the spec.
 */
public record Cluster(ClusterSpec spec, ClusterStatus status) {

    public ClusterId id() {
        return spec.id();
    }
}
