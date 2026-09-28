/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
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
  void isStartGateClosed_withoutStartGateAnnotations_returnsFalse() {
    StackGresCluster cluster = Fixtures.cluster().loadDefault().get();
    assertFalse(PatroniReconciliator.isStartGateClosed(cluster));
  }

  @Test
  void isStartGateClosed_withMissingOrDifferentAnnotations_returnsTrue() {
    StackGresCluster cluster = clusterWithStartGateAnnotations(Map.of("a", "1", "b", "2"));
    cluster.getMetadata().setAnnotations(Map.of("a", "1"));
    assertTrue(PatroniReconciliator.isStartGateClosed(cluster));
    cluster.getMetadata().setAnnotations(Map.of("a", "1", "b", "3"));
    assertTrue(PatroniReconciliator.isStartGateClosed(cluster));
    cluster.getMetadata().setAnnotations(null);
    assertTrue(PatroniReconciliator.isStartGateClosed(cluster));
  }

  @Test
  void isStartGateClosed_withLabelsInsteadOfAnnotations_returnsTrue() {
    StackGresCluster cluster = clusterWithStartGateAnnotations(Map.of("a", "1"));
    cluster.getMetadata().setAnnotations(null);
    cluster.getMetadata().setLabels(Map.of("a", "1"));
    assertTrue(PatroniReconciliator.isStartGateClosed(cluster));
  }

  @Test
  void isStartGateClosed_withAllAnnotations_returnsFalse() {
    StackGresCluster cluster = clusterWithStartGateAnnotations(Map.of("a", "1", "b", "2"));
    cluster.getMetadata().setAnnotations(Map.of("a", "1", "b", "2", "c", "3"));
    assertFalse(PatroniReconciliator.isStartGateClosed(cluster));
  }

  private StackGresCluster clusterWithStartGateAnnotations(Map<String, String> startGateAnnotations) {
    StackGresCluster cluster = Fixtures.cluster().loadDefault().get();
    if (cluster.getSpec().getConfigurations() == null) {
      cluster.getSpec().setConfigurations(new StackGresClusterConfigurations());
    }
    cluster.getSpec().getConfigurations().setPatroni(new StackGresClusterPatroni());
    cluster.getSpec().getConfigurations().getPatroni().setStartGateAnnotations(startGateAnnotations);
    return cluster;
  }

}
