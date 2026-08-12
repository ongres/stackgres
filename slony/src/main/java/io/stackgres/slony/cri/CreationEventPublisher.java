package io.stackgres.slony.cri;

import java.util.UUID;

public interface CreationEventPublisher {

    void imagePulled(UUID instanceId, String imageRef, String imageDigest);

    void imageCached(UUID instanceId, String imageRef, String imageDigest);

    void etcdImagePulled(UUID instanceId, String imageRef);

    void etcdContainerStarted(UUID instanceId);

    void etcdHealthy(UUID clusterId);

    void etcdStartupTimeout(UUID clusterId);

}