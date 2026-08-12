package io.stackgres.matriarch.spi;

import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.status.ClusterStatus;

/**
 * Observed cluster status (§3.2): a <strong>lossy cache / restart accelerant, never the
 * source of truth</strong>. Rebuilt from agents/executors and written throttled. Separate
 * from {@link StateStore} because its durability is optional — a deployment may keep it in
 * memory while desired state is durable.
 */
public interface StatusCache {

    /**
     * The latest observed status, or {@code null} if none is cached.
     */
    ClusterStatus get(ClusterId id);

    void put(ClusterStatus status);

    void delete(ClusterId id);

}