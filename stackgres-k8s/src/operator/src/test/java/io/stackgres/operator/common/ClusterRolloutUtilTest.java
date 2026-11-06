/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.patroni.PatroniMember;
import org.junit.jupiter.api.Test;

class ClusterRolloutUtilTest {

  private static final Pod POD = new PodBuilder()
      .withNewMetadata()
      .withName("test-0")
      .endMetadata()
      .build();

  @Test
  void givenNoPendingRestart_shouldNotRequirePostgresRestartWithoutSwitchover() {
    var member = new PatroniMember();
    member.setMember("test-0");
    assertFalse(ClusterRolloutUtil.requiresPostgresRestartWithoutSwitchover(
        POD, List.of(member)));
  }

  @Test
  void givenAPendingRestartWithoutReason_shouldNotRequirePostgresRestartWithoutSwitchover() {
    assertFalse(ClusterRolloutUtil.requiresPostgresRestartWithoutSwitchover(
        POD, members(null)));
  }

  @Test
  void givenADecreasedHotStandbySensitiveParameter_shouldRequirePostgresRestartWithoutSwitchover() {
    assertTrue(ClusterRolloutUtil.requiresPostgresRestartWithoutSwitchover(
        POD, members("max_connections: 80->79")));
    assertTrue(ClusterRolloutUtil.requiresPostgresRestartWithoutSwitchover(
        POD, members("shared_buffers: 128MB->256MB\nmax_worker_processes: 8->4")));
  }

  @Test
  void givenAnIncreasedHotStandbySensitiveParameter_shouldNotRequirePostgresRestartWithoutSwitchover() {
    assertFalse(ClusterRolloutUtil.requiresPostgresRestartWithoutSwitchover(
        POD, members("max_connections: 80->81")));
  }

  @Test
  void givenADecreasedNotSensitiveParameter_shouldNotRequirePostgresRestartWithoutSwitchover() {
    assertFalse(ClusterRolloutUtil.requiresPostgresRestartWithoutSwitchover(
        POD, members("work_mem: 8->4")));
  }

  @Test
  void givenNotComparableValues_shouldNotRequirePostgresRestartWithoutSwitchover() {
    assertFalse(ClusterRolloutUtil.requiresPostgresRestartWithoutSwitchover(
        POD, members("max_connections: eighty->seventy-nine")));
  }

  @Test
  void givenAPendingRestartReasonOfAnotherMember_shouldNotRequirePostgresRestartWithoutSwitchover() {
    var member = new PatroniMember();
    member.setMember("test-1");
    member.setPendingRestart("*");
    member.setPendingRestartReason("max_connections: 80->79");
    assertFalse(ClusterRolloutUtil.requiresPostgresRestartWithoutSwitchover(
        POD, List.of(member)));
  }

  @Test
  void givenAClusterOfADbOpsThatRollsOutByItself_shouldBeManagedByTheDbOps() {
    var cluster = cluster(Map.of(
        StackGresContext.ROLLOUT_DBOPS_KEY, "major-version-upgrade",
        StackGresContext.ROLLOUT_DBOPS_MANAGED_KEY, "major-version-upgrade"));
    assertTrue(ClusterRolloutUtil.isRolloutManagedByDbOps(cluster));
    // The rollout is still allowed: the operation changes the spec of the cluster, the version of
    // postgres among the rest, and waits for the operator to apply it to the StatefulSet. Only the
    // rollout of the pods is left to the operation.
    assertTrue(ClusterRolloutUtil.isRolloutAllowed(cluster));
  }

  @Test
  void givenAClusterOfADbOpsThatIsRolledOutByTheOperator_shouldNotBeManagedByTheDbOps() {
    assertFalse(ClusterRolloutUtil.isRolloutManagedByDbOps(cluster(Map.of(
        StackGresContext.ROLLOUT_DBOPS_KEY, "restart"))));
  }

  @Test
  void givenAClusterOfADbOpsThatIsRolledOutByTheOperator_shouldAllowRollout() {
    assertTrue(ClusterRolloutUtil.isRolloutAllowed(cluster(Map.of(
        StackGresContext.ROLLOUT_DBOPS_KEY, "restart"))));
  }

  @Test
  void givenAClusterWithoutAnnotations_shouldNotAllowRollout() {
    assertFalse(ClusterRolloutUtil.isRolloutAllowed(cluster(Map.of())));
  }

  private StackGresCluster cluster(Map<String, String> annotations) {
    return new StackGresClusterBuilder()
        .withNewMetadata()
        .withName("test")
        .withAnnotations(annotations)
        .endMetadata()
        .build();
  }

  private List<PatroniMember> members(String pendingRestartReason) {
    var member = new PatroniMember();
    member.setMember("test-0");
    member.setPendingRestart("*");
    member.setPendingRestartReason(pendingRestartReason);
    return List.of(member);
  }

}
