package io.stackgres.postgres;

import java.util.UUID;

public class GenericClusterInstance extends ClusterInstance {

    public GenericClusterInstance() {
    }

    public GenericClusterInstance(UUID id, String name, String version, Integer port, String listenAddress, Status status) {
        super(id, name, version, port, listenAddress, status);
    }

}
