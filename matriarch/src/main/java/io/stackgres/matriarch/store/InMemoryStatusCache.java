package io.stackgres.matriarch.store;

import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.status.ClusterStatus;
import io.stackgres.matriarch.spi.StatusCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link StatusCache} default — an observed-status map, framework-free so any host can
 * reuse it. Observed status is rebuilt from agents/observers anyway, so losing it on restart is fine.
 */
public class InMemoryStatusCache implements StatusCache {

    private final Map<ClusterId, ClusterStatus> observed = new ConcurrentHashMap<>();

    @Override
    public ClusterStatus get(ClusterId id) {
        return observed.get(id);
    }

    @Override
    public void put(ClusterStatus status) {
        observed.put(status.id(), status);
    }

    @Override
    public void delete(ClusterId id) {
        observed.remove(id);
    }

}
