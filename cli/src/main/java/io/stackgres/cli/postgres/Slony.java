package io.stackgres.cli.postgres;

import io.stackgres.cloud.CloudEnvironment;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Slony(UUID id,
                    String hostname,
                    String os,
                    String arch,
                    String version,
                    double cpu,
                    long memory,
                    CloudEnvironment cloudEnvironment,
                    SlonyStatus status,
                    Instant lastHeartbeat,
                    Map<String, String> tags) {
}
