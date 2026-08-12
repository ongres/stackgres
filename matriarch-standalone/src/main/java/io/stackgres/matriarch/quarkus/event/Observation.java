package io.stackgres.matriarch.quarkus.event;

import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.InstanceId;
import io.stackgres.matriarch.model.status.ClusterStatus;

/**
 * CDI event model carrying observations the executors push toward the matriarch.
 * This is the ADAPTER's transport — the core library never sees CDI; it only
 * exposes the {@code Matriarch#notify*} methods.
 * The {@link ObservationBridge} maps the CDI events onto them.
 */
public sealed interface Observation {

    record Status(ClusterStatus status) implements Observation {
    }

    record Removed(ClusterId id) implements Observation {
    }

    record Failed(ClusterId id, String reason) implements Observation {
    }

    record Pending(ClusterId id, String reason) implements Observation {
    }

    record Metrics(ClusterId clusterId, InstanceId instanceId, double cpu, long memory, long storageUsed) implements Observation {
    }

}