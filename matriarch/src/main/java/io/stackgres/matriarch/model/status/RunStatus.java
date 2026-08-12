package io.stackgres.matriarch.model.status;

/**
 * The observed run state of a cluster or a single instance — the lifecycle value
 * an executor reports. Shared by {@link ClusterStatus} and {@link InstanceStatus};
 * maps to the wire {@code types.v1.ClusterStatus} enum at the adapter boundary.
 */
public enum RunStatus {UNKNOWN, PENDING, INITIALIZING, STARTING, HEALTHY, FAILED, STOPPED}
