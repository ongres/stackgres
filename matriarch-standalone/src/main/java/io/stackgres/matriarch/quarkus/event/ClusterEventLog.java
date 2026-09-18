package io.stackgres.matriarch.quarkus.event;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.ClusterId;

import java.util.List;

/**
 * Storage seam for the per-cluster domain event history behind {@code GetClusterEvents}. Deliberately
 * SEPARATE from the source-of-truth {@code StateStore}: an event history is neither re-derivable by
 * observation nor required for correctness (the core never reads it), so it is a <strong>bounded,
 * best-effort</strong> log — losing some events is acceptable, and a backend failure must never break a
 * mutation. Two implementations: in-memory (dev/default) and a durable SQLite file that survives a
 * restart.
 */
public interface ClusterEventLog {

    /** Record one event (best-effort; bounded per cluster). */
    void append(ClusterEvent event);

    /** Chronological snapshot of a cluster's events (empty if unknown). */
    List<ClusterEvent> events(ClusterId id);

    /** Drop a cluster's whole history — its cluster (and name) is gone. */
    void deleteFor(ClusterId id);

    /** Max events retained per cluster; oldest are evicted beyond this. */
    int MAX_PER_CLUSTER = 500;
}
