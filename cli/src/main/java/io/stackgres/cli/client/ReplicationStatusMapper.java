package io.stackgres.cli.client;

import io.stackgres.postgres.ReplicationStatus;

public final class ReplicationStatusMapper {

    public static ReplicationStatus mapStatus(io.stackgres.proto.cli.ReplicationStatus status) {
        return switch (status) {
            case REPLICATION_STATUS_STANDALONE -> ReplicationStatus.STANDALONE;
            case REPLICATION_STATUS_PRIMARY -> ReplicationStatus.PRIMARY;
            case REPLICATION_STATUS_REPLICA -> ReplicationStatus.REPLICA;
            case REPLICATION_STATUS_UNKNOWN, UNRECOGNIZED -> ReplicationStatus.UNKNOWN;
        };
    }

    // stackgres.api.v1 (types.v1.ReplicationStatus)
    public static ReplicationStatus mapStatus(io.stackgres.proto.types.v1.ReplicationStatus status) {
        return switch (status) {
            case REPLICATION_STATUS_STANDALONE -> ReplicationStatus.STANDALONE;
            case REPLICATION_STATUS_PRIMARY -> ReplicationStatus.PRIMARY;
            case REPLICATION_STATUS_REPLICA -> ReplicationStatus.REPLICA;
            default -> ReplicationStatus.UNKNOWN;
        };
    }

}
