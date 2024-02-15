/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.AnyType;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.patroni.PatroniMember;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

@QuarkusTest
class ClusterSwitchoverHandlerTest {

  private static final String TEST_CLUSTER_NAME = "test-cluster";
  private static final String TEST_NAMESPACE_NAME = "test-namespace";

  @Inject
  ClusterSwitchoverHandler switchoverHandler;

  @InjectMock
  PatroniApiHandler patroniApiHandler;

  @Test
  void switchover_shouldScanTheMembersBeforeDoASwitchOver() {
    final PatroniMember leader = new PatroniMember();
    leader.setMember("member-0");
    leader.setCluster(TEST_CLUSTER_NAME);
    leader.setState(PatroniMember.RUNNING);
    leader.setRole(PatroniMember.LEADER);
    leader.setTimeline("1");
    final PatroniMember replica = new PatroniMember();
    replica.setMember("member-1");
    replica.setCluster(TEST_CLUSTER_NAME);
    replica.setState(PatroniMember.RUNNING);
    replica.setRole(PatroniMember.REPLICA);
    replica.setTimeline("1");
    replica.setLagInMb(new IntOrString(0));
    when(patroniApiHandler.getClusterMembers(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME))
        .thenReturn(Uni.createFrom().item(List.of(
            leader,
            replica)));

    when(patroniApiHandler.performSwitchover(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME, leader, replica))
        .thenReturn(Uni.createFrom().voidItem());

    switchoverHandler.performSwitchover(leader.getMember(), TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME)
        .await().indefinitely();

    InOrder order = Mockito.inOrder(patroniApiHandler);

    order.verify(patroniApiHandler).getClusterMembers(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME);
    order.verify(patroniApiHandler).performSwitchover(any(), any(), any(), any());
  }

  @Test
  void switchover_shouldPickTheRunningReplicaWithLeastAmountOfLag() {
    final PatroniMember leader = new PatroniMember();
    leader.setMember("member-0");
    leader.setCluster(TEST_CLUSTER_NAME);
    leader.setState(PatroniMember.RUNNING);
    leader.setRole(PatroniMember.LEADER);
    leader.setTimeline("1");
    final PatroniMember replica = new PatroniMember();
    replica.setMember("member-1");
    replica.setCluster(TEST_CLUSTER_NAME);
    replica.setState(PatroniMember.RUNNING);
    replica.setRole(PatroniMember.REPLICA);
    replica.setTimeline("1");
    replica.setLagInMb(new IntOrString(1));
    final PatroniMember candidate = new PatroniMember();
    candidate.setMember("member-2");
    candidate.setCluster(TEST_CLUSTER_NAME);
    candidate.setState(PatroniMember.RUNNING);
    candidate.setRole(PatroniMember.REPLICA);
    candidate.setTimeline("1");
    candidate.setLagInMb(new IntOrString(0));
    final PatroniMember stoppedReplica = new PatroniMember();
    stoppedReplica.setMember("member-3");
    stoppedReplica.setCluster(TEST_CLUSTER_NAME);
    stoppedReplica.setState(PatroniMember.STOPPED);
    stoppedReplica.setRole(PatroniMember.REPLICA);
    final PatroniMember initializingReplica = new PatroniMember();
    initializingReplica.setMember("member-4");
    initializingReplica.setCluster(TEST_CLUSTER_NAME);
    initializingReplica.setRole(PatroniMember.REPLICA);

    List<PatroniMember> members = new java.util.ArrayList<>(List
        .of(leader, replica, candidate, stoppedReplica, initializingReplica));

    Collections.shuffle(members);

    when(patroniApiHandler.getClusterMembers(any(), any()))
        .thenReturn(Uni.createFrom().item(members));

    when(patroniApiHandler.performSwitchover(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME, leader, candidate))
        .thenReturn(Uni.createFrom().voidItem());

    switchoverHandler.performSwitchover(leader.getMember(), TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME)
        .await().indefinitely();

    verify(patroniApiHandler).getClusterMembers(any(), any());
    verify(patroniApiHandler).performSwitchover(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME, leader, candidate);

  }

  @Test
  void switchoverWithASingleMember_shouldNotBeExecuted() {
    final PatroniMember leader = new PatroniMember();
    leader.setMember("member-0");
    leader.setCluster(TEST_CLUSTER_NAME);
    leader.setState(PatroniMember.RUNNING);
    leader.setRole(PatroniMember.LEADER);
    leader.setTimeline("1");

    when(patroniApiHandler.getClusterMembers(any(), any()))
        .thenReturn(Uni.createFrom().item(List.of(leader)));

    switchoverHandler.performSwitchover("member-0", TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME)
        .await().indefinitely();

    verify(patroniApiHandler).getClusterMembers(any(), any());
    verify(patroniApiHandler, never()).performSwitchover(any(), any(), any(), any());
  }

  @Test
  void switchoverWithNoHealthyReplicas_switchoverShouldBeSkipped() {
    final PatroniMember leader = new PatroniMember();
    leader.setMember("member-0");
    leader.setCluster(TEST_CLUSTER_NAME);
    leader.setState(PatroniMember.RUNNING);
    leader.setRole(PatroniMember.LEADER);
    leader.setTimeline("1");
    final PatroniMember replica = new PatroniMember();
    replica.setMember("member-1");
    replica.setCluster(TEST_CLUSTER_NAME);
    replica.setState(PatroniMember.STOPPED);
    replica.setRole(PatroniMember.REPLICA);
    replica.setTimeline("1");
    final PatroniMember noFilover = new PatroniMember();
    noFilover.setMember("member-2");
    noFilover.setCluster(TEST_CLUSTER_NAME);
    noFilover.setState(PatroniMember.RUNNING);
    noFilover.setRole(PatroniMember.REPLICA);
    noFilover.setTimeline("1");
    noFilover.setTags(Map.of(PatroniUtil.NOFAILOVER_TAG, new AnyType(true)));

    when(patroniApiHandler.getClusterMembers(any(), any()))
        .thenReturn(Uni.createFrom().item(List.of(leader, replica, noFilover)));

    switchoverHandler.performSwitchover("member-0", TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME)
        .await().indefinitely();

    verify(patroniApiHandler).getClusterMembers(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME);
    verify(patroniApiHandler, never()).performSwitchover(any(), any(), any(), any());
  }

  @Test
  void ifTheLeaderNameDoesNotMatch_switchoverShouldBeSkipped() {
    final PatroniMember leader = new PatroniMember();
    leader.setMember("member-0");
    leader.setCluster(TEST_CLUSTER_NAME);
    leader.setState(PatroniMember.RUNNING);
    leader.setRole(PatroniMember.LEADER);
    leader.setTimeline("1");
    final PatroniMember replica = new PatroniMember();
    replica.setMember("member-1");
    replica.setCluster(TEST_CLUSTER_NAME);
    replica.setState(PatroniMember.RUNNING);
    replica.setRole(PatroniMember.REPLICA);
    replica.setTimeline("1");
    replica.setLagInMb(new IntOrString(0));
    when(patroniApiHandler.getClusterMembers(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME))
        .thenReturn(Uni.createFrom().item(List.of(
            leader,
            replica)));

    when(patroniApiHandler.performSwitchover(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME, leader, replica))
        .thenReturn(Uni.createFrom().voidItem());

    switchoverHandler.performSwitchover(replica.getMember(), TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME)
        .await().indefinitely();

    verify(patroniApiHandler).getClusterMembers(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME);

    verify(patroniApiHandler, never()).performSwitchover(any(), any(), any(), any());

  }

}
