package io.stackgres.matriarch.quarkus.event;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.ClusterId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded in-memory history of the domain {@link ClusterEvent}s the matriarch raises, per cluster,
 * so {@code GetClusterEvents} can serve a snapshot. Subscribes to the same CDI channel as
 * {@link EventLogger}. Not durable — the history covers only this matriarch session (a durable
 * event log lands with the durable StateStore).
 */
@ApplicationScoped
public class ClusterEventStore {

    private static final int MAX_PER_CLUSTER = 500;

    private final Map<ClusterId, Deque<ClusterEvent>> byCluster = new ConcurrentHashMap<>();

    void onClusterEvent(@Observes ClusterEvent event) {
        if (event instanceof ClusterEvent.ClusterDeleted) {
            // The cluster (and its name) is gone — its history is no longer queryable by name, so drop it.
            byCluster.remove(event.clusterId());
            return;
        }
        Deque<ClusterEvent> deque = byCluster.computeIfAbsent(event.clusterId(), k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(event);
            while (deque.size() > MAX_PER_CLUSTER) {
                deque.removeFirst();
            }
        }
    }

    /**
     * Chronological snapshot of a cluster's events (empty if the cluster is unknown).
     */
    public List<ClusterEvent> events(ClusterId id) {
        Deque<ClusterEvent> deque = byCluster.get(id);
        if (deque == null) {
            return List.of();
        }
        synchronized (deque) {
            return new ArrayList<>(deque);
        }
    }

}