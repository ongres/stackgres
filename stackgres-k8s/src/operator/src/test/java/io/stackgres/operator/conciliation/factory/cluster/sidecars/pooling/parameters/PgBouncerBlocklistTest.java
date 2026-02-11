/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class PgBouncerBlocklistTest {

  @Test
  void getBlocklistParameters_shouldReturnNonEmptySet() {
    Set<String> blocklist = PgBouncerBlocklist.getBlocklistParameters();

    assertFalse(blocklist.isEmpty());
  }

  @Test
  void getBlocklistParameters_shouldContainAdminUsers() {
    Set<String> blocklist = PgBouncerBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("admin_users"),
        "Expected blocklist to contain 'admin_users'");
  }

  @Test
  void getBlocklistParameters_shouldContainAuthFile() {
    Set<String> blocklist = PgBouncerBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("auth_file"),
        "Expected blocklist to contain 'auth_file'");
  }

  @Test
  void getBlocklistParameters_shouldContainListenAddr() {
    Set<String> blocklist = PgBouncerBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("listen_addr"),
        "Expected blocklist to contain 'listen_addr'");
  }

  @Test
  void getBlocklistParameters_shouldContainListenPort() {
    Set<String> blocklist = PgBouncerBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("listen_port"),
        "Expected blocklist to contain 'listen_port'");
  }

  @Test
  void getBlocklistParameters_shouldContainLogfile() {
    Set<String> blocklist = PgBouncerBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("logfile"),
        "Expected blocklist to contain 'logfile'");
  }

  @Test
  void getBlocklistParameters_shouldContainUnixSocketDir() {
    Set<String> blocklist = PgBouncerBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("unix_socket_dir"),
        "Expected blocklist to contain 'unix_socket_dir'");
  }

  @Test
  void getBlocklistParameters_shouldContainAuthType() {
    Set<String> blocklist = PgBouncerBlocklist.getBlocklistParameters();

    assertTrue(blocklist.contains("auth_type"),
        "Expected blocklist to contain 'auth_type'");
  }

  @Test
  void getBlocklistParameters_shouldReturnImmutableSet() {
    Set<String> blocklist = PgBouncerBlocklist.getBlocklistParameters();

    try {
      blocklist.add("test_parameter");
      // If we get here, the set is mutable which is unexpected
      assertFalse(true, "Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // Expected behavior - set is immutable
    }
  }
}
