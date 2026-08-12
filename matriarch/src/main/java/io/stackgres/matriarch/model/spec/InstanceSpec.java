package io.stackgres.matriarch.model.spec;
import io.stackgres.matriarch.model.InstanceId;

/**
 * Desired state of one cluster instance. {@code requestedPort} is the user's explicit port choice,
 * or {@code null} to let the substrate assign one — the slony-linux executor scans host ports from
 * its base via the ReportUnusedPort handshake; a k8s executor uses a service-local port. An explicit
 * port is <em>strict</em>: provisioning fails if it is already in use. The actual bound port lands in
 * {@link io.stackgres.matriarch.model.status.InstanceStatus#port()}.
 */
public record InstanceSpec(
        InstanceId id,
        InstanceRole role,
        Integer requestedPort,
        String listenAddress,
        EngineSpec engineSpec) {
}
