package io.stackgres.cli.postgres;

import java.time.Instant;
import java.util.UUID;

public record InstanceDiagnostics(UUID id, String name, String pgControlData, Instant receivedAt, String imageName, String imageDigest) {
}
