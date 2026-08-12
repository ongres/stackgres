package io.stackgres.cli.client;

import io.stackgres.postgres.Status;
import io.stackgres.proto.cli.ClusterStatus;

public final class ClusterStatusMapper {

    public static Status mapStatus(ClusterStatus status) {
        return switch (status) {
            case STATUS_CREATED -> Status.CREATED;
            case STATUS_INITDB -> Status.INITDB;
            case STATUS_STARTED -> Status.STARTED;
            case STATUS_HEALTHY -> Status.HEALTHY;
            case STATUS_FAILED -> Status.CRASHED;
            case STATUS_STOPPED -> Status.STOPPED;
            case STATUS_UNKNOWN -> Status.UNKNOWN;
            default -> throw new IllegalArgumentException("Unknown status type: " + status);
        };
    }

    // stackgres.api.v1 (types.v1.ClusterStatus)
    public static Status mapStatus(io.stackgres.proto.types.v1.ClusterStatus status) {
        return switch (status) {
            case CLUSTER_STATUS_CREATED -> Status.CREATED;
            case CLUSTER_STATUS_PENDING -> Status.PENDING;
            case CLUSTER_STATUS_INITIALIZING -> Status.INITDB;
            case CLUSTER_STATUS_STARTED -> Status.STARTED;
            case CLUSTER_STATUS_HEALTHY -> Status.HEALTHY;
            case CLUSTER_STATUS_FAILED -> Status.CRASHED;
            case CLUSTER_STATUS_STOPPED -> Status.STOPPED;
            default -> Status.UNKNOWN;
        };
    }

}