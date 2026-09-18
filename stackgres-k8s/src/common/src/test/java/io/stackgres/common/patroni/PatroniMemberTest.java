/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import io.stackgres.common.patroni.PatroniMember.PendingRestartReason;
import org.junit.jupiter.api.Test;

class PatroniMemberTest {

  @Test
  void givenNoPendingRestartReason_shouldReturnAnEmptyMap() {
    assertTrue(new PatroniMember().getPendingRestartReasons().isEmpty());
  }

  @Test
  void givenAPendingRestartReason_shouldReturnTheParameterValues() {
    var member = new PatroniMember();
    member.setPendingRestartReason("max_connections: 80->79");

    assertEquals(
        Map.of("max_connections", new PendingRestartReason("80", "79")),
        member.getPendingRestartReasons());
  }

  @Test
  void givenAPendingRestartReasonWithManyParameters_shouldReturnAllTheParameterValues() {
    var member = new PatroniMember();
    member.setPendingRestartReason(
        "max_connections: 80->79\nshared_buffers: 128MB->256MB\nmax_wal_senders: 10->5");

    assertEquals(
        Map.of(
            "max_connections", new PendingRestartReason("80", "79"),
            "shared_buffers", new PendingRestartReason("128MB", "256MB"),
            "max_wal_senders", new PendingRestartReason("10", "5")),
        member.getPendingRestartReasons());
  }

  @Test
  void givenAPendingRestartReasonWithAnEmptyValue_shouldReturnTheParameterValues() {
    var member = new PatroniMember();
    member.setPendingRestartReason("max_connections: ->79");

    assertEquals(
        Map.of("max_connections", new PendingRestartReason("", "79")),
        member.getPendingRestartReasons());
  }

  @Test
  void givenAnUnparseablePendingRestartReason_shouldIgnoreIt() {
    var member = new PatroniMember();
    member.setPendingRestartReason("max_connections\nmax_wal_senders: 10->5");

    assertEquals(
        Map.of("max_wal_senders", new PendingRestartReason("10", "5")),
        member.getPendingRestartReasons());
  }

}
