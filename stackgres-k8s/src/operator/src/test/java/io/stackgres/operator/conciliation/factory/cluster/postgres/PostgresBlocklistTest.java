/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.postgres;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class PostgresBlocklistTest {

  @Test
  void getBlocklistParameters_shouldReturnNonEmptySet() {
    Set<String> blocklist = PostgresBlocklist.getBlocklistParameters();

    assertFalse(blocklist.isEmpty());
  }

  @Test
  void getBlocklistParameters_shouldContainListenAddresses() {
    Set<String> blocklist = PostgresBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("listen_addresses"),
        "Expected blocklist to contain 'listen_addresses'");
  }

  @Test
  void getBlocklistParameters_shouldContainPort() {
    Set<String> blocklist = PostgresBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("port"),
        "Expected blocklist to contain 'port'");
  }

  @Test
  void getBlocklistParameters_shouldContainLoggingCollector() {
    Set<String> blocklist = PostgresBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("logging_collector"),
        "Expected blocklist to contain 'logging_collector'");
  }

  @Test
  void getBlocklistParameters_shouldContainArchiveMode() {
    Set<String> blocklist = PostgresBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("archive_mode"),
        "Expected blocklist to contain 'archive_mode'");
  }

  @Test
  void getBlocklistParameters_shouldContainArchiveCommand() {
    Set<String> blocklist = PostgresBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("archive_command"),
        "Expected blocklist to contain 'archive_command'");
  }

  @Test
  void getBlocklistParameters_shouldReturnImmutableSet() {
    Set<String> blocklist = PostgresBlocklist.getBlocklistParameters();

    try {
      blocklist.add("test_parameter");
      // If we get here, the set is mutable which is unexpected
      assertFalse(true, "Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // Expected behavior - set is immutable
    }
  }
}
