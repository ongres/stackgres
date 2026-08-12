package io.stackgres.operator.matriarch;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.ClusterId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Bounded in-memory history of the domain {@link ClusterEvent}s the matriarch raises, per cluster,
 * so {@code GetClusterEvents} can serve a snapshot. Fed by the core's CDI event hook (wired in
 * {@link MatriarchProducer}); the {@link StackGresObserver} produces the events (recovered on first
 * sight, healthy/failed/... on status change, deleted on removal). Not durable — this session only.
 */
@ApplicationScoped
public class ClusterEventStore {

  private static final int MAX_PER_CLUSTER = 500;

  private final Map<ClusterId, Deque<ClusterEvent>> byCluster = new ConcurrentHashMap<>();

  void onClusterEvent(@Observes ClusterEvent event) {
    if (event instanceof ClusterEvent.ClusterDeleted) {
      // The cluster is gone — its history is no longer queryable, so drop it.
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

  /** Chronological snapshot of a cluster's events (empty if the cluster is unknown). */
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
