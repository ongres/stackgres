package io.stackgres.postgres;

import java.util.UUID;

public class SlonClusterInstance extends ClusterInstance {

    public SlonClusterInstance() {
    }

    public SlonClusterInstance(UUID id, String name, String version, Integer port, String listenAddress) {
        super(id, name, version, port, listenAddress, Status.CREATED);
    }

}