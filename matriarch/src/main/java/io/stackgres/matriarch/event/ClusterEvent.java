package io.stackgres.matriarch.event;

import io.stackgres.matriarch.model.ClusterId;

import java.time.Instant;

/**
 * Cluster-lifecycle events — exhaustive over the lifecycle the matriarch drives.
 * (Start/Stop/Restart join when those verbs land; an {@code InstanceEvent} sibling
 * joins when slony/slon instance events are surfaced.)
 */
public sealed interface ClusterEvent extends Event {

    ClusterId clusterId();

    record ClusterAccepted(Instant timestamp, ClusterId clusterId, String name, String version, boolean standalone) implements ClusterEvent {
    }

    record ClusterHealthy(Instant timestamp, ClusterId clusterId) implements ClusterEvent {
    }

    record ClusterFailed(Instant timestamp, ClusterId clusterId, String reason) implements ClusterEvent {
    }

    record ClusterStarting(Instant timestamp, ClusterId clusterId) implements ClusterEvent {
    }

    record ClusterStopping(Instant timestamp, ClusterId clusterId) implements ClusterEvent {
    }

    record ClusterRestarting(Instant timestamp, ClusterId clusterId) implements ClusterEvent {
    }

    record ClusterDeleting(Instant timestamp, ClusterId clusterId) implements ClusterEvent {
    }

    record ClusterDeleted(Instant timestamp, ClusterId clusterId) implements ClusterEvent {
    }

    record ClusterRecovered(Instant timestamp, ClusterId clusterId, String name) implements ClusterEvent {
    }

    /** A read-only observer saw a cluster it did not create for the first time (vs. {@link ClusterRecovered}, agent re-report after restart). */
    record ClusterObserved(Instant timestamp, ClusterId clusterId, String name) implements ClusterEvent {
    }

}