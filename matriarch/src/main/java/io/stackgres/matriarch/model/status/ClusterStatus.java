package io.stackgres.matriarch.model.status;

import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.spec.ClusterSpec;

import java.util.List;

/**
 * Observed cluster status — rebuilt from agents/executors, persisted only as a
 * restart accelerant, never the source of truth (§3.2). The runtime counterpart
 * to {@link ClusterSpec} (desired): the overall {@link RunStatus} plus per-instance
 * detail. Instance-centric.
 */
public record ClusterStatus(ClusterId id, RunStatus runStatus, List<InstanceStatus> instances) {

    public static ClusterStatus unknown(ClusterId id) {
        return new ClusterStatus(id, RunStatus.UNKNOWN, List.of());
    }

    public static ClusterStatus pending(ClusterId id) {
        return new ClusterStatus(id, RunStatus.PENDING, List.of());
    }
}
