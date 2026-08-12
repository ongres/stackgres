package io.stackgres.matriarch.model.spec;

import java.util.Map;

/**
 * A user's create intent (the engine-independent shape an adapter maps the
 * api.v1 {@code CreateClusterRequest} onto). The matriarch <em>resolves</em> this
 * into a concrete {@link ClusterSpec} at accept time — assigning ids, expanding
 * {@code replicas} into instances, and defaulting placement. For the first
 * end-to-end attempt only {@code replicas == 0} (standalone, one PRIMARY) is
 * supported.
 */
public record ClusterCreate(
        String name,
        DatabaseEngine engine,
        String version,
        int replicas,
        Integer requestedPort,
        String listenAddress,
        String username,
        String password,
        TlsMode tls,
        EngineSpec engineSpec,
        Map<String, String> tags) {
}
