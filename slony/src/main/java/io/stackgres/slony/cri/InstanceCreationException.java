package io.stackgres.slony.cri;

import java.util.UUID;

public class InstanceCreationException extends RuntimeException {

    private final UUID instanceId;

    public InstanceCreationException(UUID instanceId, Exception cause) {
        super(cause.getMessage(), cause);
        this.instanceId = instanceId;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

}