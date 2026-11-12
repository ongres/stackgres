/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.YamlScalarWriter;
import org.junit.jupiter.api.Test;

class PatroniConfigCredentialsTest {

  // As written by start-patroni.sh
  private static final List<String> CONFIG_LINES = List.of(
      "restapi:",
      "  connect_address: '10.0.0.1:8008'",
      "  listen: \"*:8008\"",
      "  authentication: { username: 'superuser', password: 'api''s' }",
      "postgresql:",
      "  use_slots: true",
      "  authentication: { superuser: { username: 'postgres', password: 'a\"b:{c}, d#e' },"
          + " replication: { username: 'replicator', password: 'r' } }",
      "  parameters:",
      "tags: {}");

  private final PatroniConfigCredentials patroniConfigCredentials =
      new PatroniConfigCredentials(new YamlMapperProvider(),
            new YamlScalarWriter(new YamlMapperProvider()));

  @Test
  void getCredentials_fromTheLinesWrittenByThePatroniContainer() {
    assertEquals(Optional.of(new PatroniCommandUtil.Credentials("superuser", "api's")),
        patroniConfigCredentials.getRestApi(CONFIG_LINES));
    assertEquals(Optional.of(new PostgresUtil.Credentials("postgres", "a\"b:{c}, d#e")),
        patroniConfigCredentials.getSuperuser(CONFIG_LINES));
    assertEquals(Optional.of(new PostgresUtil.Credentials("replicator", "r")),
        patroniConfigCredentials.getReplication(CONFIG_LINES));
  }

  @Test
  void getCredentials_withoutAuthenticationLines_returnsEmpty() {
    List<String> lines = List.of("restapi:", "  listen: \"*:8008\"", "postgresql:",
        "  authentication:", "    superuser:", "      username: 'postgres'");
    assertTrue(patroniConfigCredentials.getRestApi(lines).isEmpty());
    assertTrue(patroniConfigCredentials.getSuperuser(lines).isEmpty());
    assertTrue(patroniConfigCredentials.getReplication(lines).isEmpty());
  }

  @Test
  void replaceLine_replacesTheLinesOfTheCredentialsWithLinesThatReadAsTheNewOnes() {
    final String longPassword = "p'\"\\:{}[],#&*!|>%@` \n\t\007ñ😀 ".repeat(10);
    List<String> lines = patroniConfigCredentials.replaceLine(
        CONFIG_LINES,
        PatroniConfigCredentials.POSTGRESQL_AUTHENTICATION_LINE_PATTERN,
        patroniConfigCredentials.getPostgresqlAuthenticationLine(
            new PostgresUtil.Credentials("postgres2", longPassword),
            new PostgresUtil.Credentials("replicator2", "r2")));
    lines = patroniConfigCredentials.replaceLine(
        lines,
        PatroniConfigCredentials.RESTAPI_AUTHENTICATION_LINE_PATTERN,
        patroniConfigCredentials.getRestApiAuthenticationLine(
            new PatroniCommandUtil.Credentials("superuser", "api2")));

    assertEquals(CONFIG_LINES.size(), lines.size());
    assertFalse(lines.stream().anyMatch(line -> line.contains("\n")), lines.toString());
    assertEquals(Optional.of(new PostgresUtil.Credentials("postgres2", longPassword)),
        patroniConfigCredentials.getSuperuser(lines));
    assertEquals(Optional.of(new PostgresUtil.Credentials("replicator2", "r2")),
        patroniConfigCredentials.getReplication(lines));
    assertEquals(Optional.of(new PatroniCommandUtil.Credentials("superuser", "api2")),
        patroniConfigCredentials.getRestApi(lines));
    assertEquals(CONFIG_LINES.get(5), lines.get(5));
    assertEquals(CONFIG_LINES.get(7), lines.get(7));
  }

  @Test
  void replaceLine_withoutMatchingLine_returnsTheSameLines() {
    List<String> lines = List.of("restapi:", "  listen: \"*:8008\"");
    assertEquals(lines, patroniConfigCredentials.replaceLine(lines,
        PatroniConfigCredentials.RESTAPI_AUTHENTICATION_LINE_PATTERN, "  authentication: {}"));
  }

}
