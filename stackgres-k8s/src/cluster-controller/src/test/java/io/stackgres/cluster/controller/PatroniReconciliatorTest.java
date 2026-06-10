/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
