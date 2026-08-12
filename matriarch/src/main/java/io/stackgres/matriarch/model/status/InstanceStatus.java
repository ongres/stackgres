package io.stackgres.matriarch.model.status;
import io.stackgres.matriarch.model.InstanceId;

/**
 * Observed state of one instance — rebuilt from slon/slony reports. {@code cpu}/{@code memory} are
 * the host totals from the slony registration; {@code storageUsed} is the database size pushed by the
 * slon in periodic Diagnostics.
 */
public record InstanceStatus(
        InstanceId id,
        RunStatus runStatus,
        ReplicationStatus replication,
        String address,
        int port,
        double cpu,
        long memory,
        long storageUsed) {

    public static InstanceStatus pending(InstanceId id) {
        return new InstanceStatus(id, RunStatus.UNKNOWN, ReplicationStatus.UNKNOWN, "", 0, 0, 0, 0);
    }
}
