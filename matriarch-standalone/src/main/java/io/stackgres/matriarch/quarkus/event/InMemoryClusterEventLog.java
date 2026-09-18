package io.stackgres.matriarch.quarkus.event;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.ClusterId;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link ClusterEventLog} — a bounded per-cluster ring, non-durable (history covers only this
 * matriarch session). The default; a durable backend swaps in with no change to callers.
 */
public class InMemoryClusterEventLog implements ClusterEventLog {

    private final Map<ClusterId, Deque<ClusterEvent>> byCluster = new ConcurrentHashMap<>();

    @Override
    public void append(ClusterEvent event) {
        Deque<ClusterEvent> deque = byCluster.computeIfAbsent(event.clusterId(), k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(event);
            while (deque.size() > MAX_PER_CLUSTER) {
                deque.removeFirst();
            }
        }
    }

    @Override
    public List<ClusterEvent> events(ClusterId id) {
        Deque<ClusterEvent> deque = byCluster.get(id);
        if (deque == null) {
            return List.of();
        }
        synchronized (deque) {
            return new ArrayList<>(deque);
        }
    }

    @Override
    public void deleteFor(ClusterId id) {
        byCluster.remove(id);
    }
}
