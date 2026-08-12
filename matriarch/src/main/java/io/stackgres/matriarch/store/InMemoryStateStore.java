package io.stackgres.matriarch.store;

import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.spi.StateStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link StateStore} default — {@code ConcurrentHashMap}s, framework-free so any host
 * (the standalone app, the StackGres operator, tests) can reuse it by producing it as a bean or
 * {@code new}-ing it directly. Not durable; a real backend (SQLite/etcd/ConfigMaps) swaps in with
 * no change to the core.
 */
public class InMemoryStateStore implements StateStore {

    private final Map<ClusterId, ClusterSpec> desired = new ConcurrentHashMap<>();
    private final Map<String, ClusterId> idempotency = new ConcurrentHashMap<>();
    private final Map<ClusterId, String> credentials = new ConcurrentHashMap<>();

    @Override
    public ClusterSpec getDesired(ClusterId id) {
        return desired.get(id);
    }

    @Override
    public List<ClusterSpec> listDesired() {
        return new ArrayList<>(desired.values());
    }

    @Override
    public void createDesired(ClusterSpec spec, String idempotencyKey) {
        desired.compute(spec.id(), (id, current) -> {
            if (current != null) {
                throw new IllegalStateException("cluster already exists: " + id.value());
            }
            return spec;
        });
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotency.put(idempotencyKey, spec.id());
        }
    }

    @Override
    public void updateDesired(ClusterSpec spec) {
        desired.put(spec.id(), spec);
    }

    @Override
    public void deleteDesired(ClusterId id) {
        desired.remove(id);
        credentials.remove(id);
        // A deleted cluster's idempotency keys must not linger, or a later create with the same key
        // (e.g. the same name) would match a cluster that no longer exists.
        idempotency.values().removeIf(id::equals);
    }

    @Override
    public void putCredential(ClusterId id, String password) {
        credentials.put(id, password);
    }

    @Override
    public String getCredential(ClusterId id) {
        return credentials.get(id);
    }

    @Override
    public ClusterId findByIdempotencyKey(String key) {
        return key == null || key.isBlank() ? null : idempotency.get(key);
    }

    @Override
    public boolean recordIdempotency(String key, ClusterId clusterId) {
        return idempotency.putIfAbsent(key, clusterId) == null;
    }

}
