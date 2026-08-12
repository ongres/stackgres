package io.stackgres.matriarch.model;

/**
 * One frame of an accepted-then-watch lifecycle operation (§3.6). Two axes, kept
 * separate: {@code status} is the OPERATION's status; {@code cluster} is the
 * current snapshot (carrying its own {@link io.stackgres.matriarch.model.status.ClusterStatus} and per-instance
 * detail). {@code error} is the failure reason — {@code null} unless {@code status}
 * is FAILED (absent, not empty).
 */
public record ClusterOperationProgress(OperationStatus status, Cluster cluster, String error) {

    public enum OperationStatus {ACCEPTED, RUNNING, SUCCEEDED, FAILED}

    public static ClusterOperationProgress accepted(Cluster cluster) {
        return new ClusterOperationProgress(OperationStatus.ACCEPTED, cluster, null);
    }

    public static ClusterOperationProgress running(Cluster cluster) {
        return new ClusterOperationProgress(OperationStatus.RUNNING, cluster, null);
    }

    public static ClusterOperationProgress succeeded(Cluster cluster) {
        return new ClusterOperationProgress(OperationStatus.SUCCEEDED, cluster, null);
    }

    public static ClusterOperationProgress failed(Cluster cluster, String error) {
        return new ClusterOperationProgress(OperationStatus.FAILED, cluster, error);
    }

}