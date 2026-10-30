/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresReplicationRole;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.Test;

class PatroniReconciliatorTest {

  @Test
  void escapeSedReplacement_escapesAmpersand() {
    // & means "the whole matched text" in a sed replacement, so it must be escaped.
    assertEquals("  pre_promote: check_a \\&\\& check_b",
        PatroniReconciliator.escapeSedReplacement("  pre_promote: check_a && check_b"));
  }

  @Test
  void escapeSedReplacement_escapesSlashAndBackslash() {
    assertEquals("  before_stop: \\/usr\\/bin\\/stop.sh \\\\",
        PatroniReconciliator.escapeSedReplacement("  before_stop: /usr/bin/stop.sh \\"));
  }

  @Test
  void escapeSedReplacement_escapesNewline() {
    assertEquals("a\\nb", PatroniReconciliator.escapeSedReplacement("a\nb"));
  }

  @Test
  void escapeSedReplacement_leavesPlainValueUnchanged() {
    assertEquals("  pg_ctl_timeout: 60",
        PatroniReconciliator.escapeSedReplacement("  pg_ctl_timeout: 60"));
  }

  @Test
  void getPodAssignedReplicationRole_ofAPodOfTheCluster_returnsTheRoleOfItsGroup() {
    StackGresCluster cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().setInstances(2);
    assertEquals(StackGresReplicationRole.HA_READ,
        PatroniReconciliator.getPodAssignedReplicationRole(cluster, "test-1").orElseThrow().v1);
  }

  @Test
  void getPodAssignedReplicationRole_ofAPodBeyondTheInstances_returnsEmpty() {
    // The additional Pod of a rollout with the reduced impact method belongs to no replication
    // group, so it has no tags to be written and its Patroni has to be allowed to start anyway.
    StackGresCluster cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().setInstances(2);
    assertTrue(
        PatroniReconciliator.getPodAssignedReplicationRole(cluster, "test-2").isEmpty());
  }
}
