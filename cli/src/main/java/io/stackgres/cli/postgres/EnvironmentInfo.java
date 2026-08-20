package io.stackgres.cli.postgres;

import java.time.Instant;
import java.util.List;

/**
 * A registered environment as the CLI renders it: one row of {@code stackgres environment list}. A
 * local matriarch returns exactly one (itself); the cloud returns the whole fleet. {@code source} is
 * LIVE while the owning matriarch is connected, CACHED once it disconnects; {@code asOf} is the last
 * time the server heard from it. Named {@code EnvironmentInfo} (not {@code Environment}) to avoid a
 * simple-name clash with the proto {@code io.stackgres.proto.api.v1.Environment}.
 */
public record EnvironmentInfo(String id, String kind, String source, String health,
                              Instant asOf, List<String> surfaces) {
}
