package io.stackgres.cli.postgres;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A registered environment as the CLI renders it: one row of {@code stackgres environment list}. A
 * local matriarch returns exactly one (itself); the cloud returns the whole fleet. {@code name} is the
 * environment's identifier — what {@code -E}, {@code environment use/get/rename/delete} take (unique
 * per user on the cloud, and editable there). {@code status} is Connected while the owning matriarch is
 * connected and Disconnected once it is not (its data is then the cached last-known state);
 * {@code asOf} is the last time the server heard from it. Named {@code EnvironmentInfo} (not {@code Environment}) to avoid a
 * simple-name clash with the proto {@code io.stackgres.proto.api.v1.Environment}.
 */
public record EnvironmentInfo(String name, String kind, String status,
                              Instant asOf, List<String> surfaces, Map<String, String> tags) {
}
