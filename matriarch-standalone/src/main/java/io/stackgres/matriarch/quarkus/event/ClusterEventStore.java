package io.stackgres.matriarch.quarkus.event;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.ClusterId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Subscribes to the domain {@link ClusterEvent}s the matriarch raises and records a per-cluster history
 * for {@code GetClusterEvents}. Storage is delegated to a pluggable {@link ClusterEventLog} — in-memory
 * (dev) or a durable SQLite file (survives a restart) — a separate, bounded, best-effort log, never the
 * source-of-truth state store.
 */
@ApplicationScoped
public class ClusterEventStore {

    @Inject
    ClusterEventLog log;

    void onClusterEvent(@Observes ClusterEvent event) {
        if (event instanceof ClusterEvent.ClusterDeleted) {
            // The cluster (and its name) is gone — its history is no longer queryable, so drop it.
            log.deleteFor(event.clusterId());
            return;
        }
        log.append(event);
    }

    /** Chronological snapshot of a cluster's events (empty if the cluster is unknown). */
    public List<ClusterEvent> events(ClusterId id) {
        return log.events(id);
    }
}
