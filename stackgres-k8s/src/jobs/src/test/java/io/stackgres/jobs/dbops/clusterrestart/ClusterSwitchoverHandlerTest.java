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
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.PatroniUtil;
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

    final ClusterMember leader = ImmutableClusterMember.builder()
        .name("member-0")
        .clusterName(TEST_CLUSTER_NAME)
        .namespace(TEST_NAMESPACE_NAME)
        .state(MemberState.RUNNING)
        .role(MemberRole.LEADER)
        .host("127.0.0.1")
        .apiUrl("http://127.0.0.1:8008/patroni")
        .port(7433)
        .timeline(1)
        .build();
    final ClusterMember replica = ImmutableClusterMember.builder()
        .name("member-1")
        .clusterName(TEST_CLUSTER_NAME)
        .namespace(TEST_NAMESPACE_NAME)
        .state(MemberState.RUNNING)
        .role(MemberRole.REPLICA)
        .host("127.0.0.2")
        .apiUrl("http://127.0.0.2:8008/patroni")
        .port(7433)
        .timeline(1)
        .lag(0)
        .build();
    when(patroniApiHandler.getClusterMembers(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME))
        .thenReturn(Uni.createFrom().item(List.of(
            leader,
            replica)));

    when(patroniApiHandler.performSwitchover(leader, replica))
        .thenReturn(Uni.createFrom().voidItem());

    switchoverHandler.performSwitchover(leader.getName(), TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME)
        .await().indefinitely();

    InOrder order = Mockito.inOrder(patroniApiHandler);

    order.verify(patroniApiHandler).getClusterMembers(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME);
    order.verify(patroniApiHandler).performSwitchover(any(), any());
  }

  @Test
  void switchover_shouldPickTheRunningReplicaWithLeastAmountOfLag() {

    final ClusterMember leader = ImmutableClusterMember.builder()
        .name("member-0")
        .clusterName(TEST_CLUSTER_NAME)
        .namespace(TEST_NAMESPACE_NAME)
        .state(MemberState.RUNNING)
        .role(MemberRole.LEADER)
        .host("127.0.0.1")
        .apiUrl("http://127.0.0.1:8008/patroni")
        .port(7433)
        .timeline(1)
        .build();

    final ClusterMember replica = ImmutableClusterMember.builder()
        .name("member-1")
        .clusterName(TEST_CLUSTER_NAME)
        .namespace(TEST_NAMESPACE_NAME)
        .state(MemberState.RUNNING)
        .role(MemberRole.REPLICA)
        .host("127.0.0.2")
        .apiUrl("http://127.0.0.2:8008/patroni")
        .port(7433)
        .timeline(1)
        .lag(1)
        .build();

    final ClusterMember candidate = ImmutableClusterMember.builder()
        .name("member-2")
        .clusterName(TEST_CLUSTER_NAME)
        .namespace(TEST_NAMESPACE_NAME)
        .state(MemberState.RUNNING)
        .role(MemberRole.REPLICA)
        .host("127.0.0.3")
        .apiUrl("http://127.0.0.3:8008/patroni")
        .port(7433)
        .timeline(1)
        .lag(0)
        .build();

    final ClusterMember stoppedReplica = ImmutableClusterMember.builder()
        .name("member-3")
        .clusterName(TEST_CLUSTER_NAME)
        .namespace(TEST_NAMESPACE_NAME)
        .state(MemberState.STOPPED)
        .role(MemberRole.REPLICA)
        .host("127.0.0.4")
        .apiUrl("http://127.0.0.4:8008/patroni")
        .port(7433)
        .build();

    final ClusterMember initializingReplica = ImmutableClusterMember.builder()
        .name("member-4")
        .clusterName(TEST_CLUSTER_NAME)
        .namespace(TEST_NAMESPACE_NAME)
        .state(Optional.empty())
        .role(MemberRole.REPLICA)
        .build();

    List<ClusterMember> members = new java.util.ArrayList<>(List
        .of(leader, replica, candidate, stoppedReplica, initializingReplica));

    Collections.shuffle(members);

    when(patroniApiHandler.getClusterMembers(any(), any()))
        .thenReturn(Uni.createFrom().item(members));

    when(patroniApiHandler.performSwitchover(leader, candidate))
        .thenReturn(Uni.createFrom().voidItem());

    switchoverHandler.performSwitchover(leader.getName(), TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME)
        .await().indefinitely();

    verify(patroniApiHandler).getClusterMembers(any(), any());
    verify(patroniApiHandler).performSwitchover(leader, candidate);

  }

  @Test
  void switchoverWithASingleMember_shouldNotBeExecuted() {

    when(patroniApiHandler.getClusterMembers(any(), any()))
        .thenReturn(Uni.createFrom().item(List.of(
            ImmutableClusterMember.builder()
                .name("member-0")
                .clusterName(TEST_CLUSTER_NAME)
                .namespace(TEST_NAMESPACE_NAME)
                .state(MemberState.RUNNING)
                .role(MemberRole.LEADER)
                .host("127.0.0.1")
                .apiUrl("http://127.0.0.1:8008/patroni")
                .port(7433)
                .timeline(1)
                .build())));

    switchoverHandler.performSwitchover("member-0", TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME)
        .await().indefinitely();

    verify(patroniApiHandler).getClusterMembers(any(), any());
    verify(patroniApiHandler, never()).performSwitchover(any(), any());
  }

  @Test
  void switchoverWithNoHealthyReplicas_switchoverShouldBeSkipped() {

    when(patroniApiHandler.getClusterMembers(any(), any()))
        .thenReturn(Uni.createFrom().item(List.of(
            ImmutableClusterMember.builder()
                .name("member-0")
                .clusterName(TEST_CLUSTER_NAME)
                .namespace(TEST_NAMESPACE_NAME)
                .state(MemberState.RUNNING)
                .role(MemberRole.LEADER)
                .host("127.0.0.1")
                .apiUrl("http://127.0.0.1:8008/patroni")
                .port(7433)
                .timeline(1)
                .build(),
            ImmutableClusterMember.builder()
                .name("member-1")
                .clusterName(TEST_CLUSTER_NAME)
                .namespace(TEST_NAMESPACE_NAME)
                .state(MemberState.STOPPED)
                .role(MemberRole.REPLICA)
                .host("127.0.0.2")
                .apiUrl("http://127.0.0.4:8008/patroni")
                .port(7433)
                .build(),
            ImmutableClusterMember.builder()
                .name("member-2")
                .clusterName(TEST_CLUSTER_NAME)
                .namespace(TEST_NAMESPACE_NAME)
                .state(MemberState.RUNNING)
                .role(MemberRole.REPLICA)
                .host("127.0.0.3")
                .apiUrl("http://127.0.0.4:8008/patroni")
                .port(7433)
                .tags(ImmutableMap.of(PatroniUtil.NOFAILOVER_TAG, PatroniUtil.TRUE_TAG_VALUE))
                .build())));

    switchoverHandler.performSwitchover("member-0", TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME)
        .await().indefinitely();

    verify(patroniApiHandler).getClusterMembers(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME);
    verify(patroniApiHandler, never()).performSwitchover(any(), any());
  }

  @Test
  void ifTheLeaderNameDoesNotMatch_switchoverShouldBeSkipped() {

    final ClusterMember leader = ImmutableClusterMember.builder()
        .name("member-0")
        .clusterName(TEST_CLUSTER_NAME)
        .namespace(TEST_NAMESPACE_NAME)
        .state(MemberState.RUNNING)
        .role(MemberRole.LEADER)
        .host("127.0.0.1")
        .apiUrl("http://127.0.0.1:8008/patroni")
        .port(7433)
        .timeline(1)
        .build();
    final ClusterMember replica = ImmutableClusterMember.builder()
        .name("member-1")
        .clusterName(TEST_CLUSTER_NAME)
        .namespace(TEST_NAMESPACE_NAME)
        .state(MemberState.RUNNING)
        .role(MemberRole.REPLICA)
        .host("127.0.0.2")
        .apiUrl("http://127.0.0.2:8008/patroni")
        .port(7433)
        .timeline(1)
        .lag(0)
        .build();
    when(patroniApiHandler.getClusterMembers(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME))
        .thenReturn(Uni.createFrom().item(List.of(
            leader,
            replica)));

    when(patroniApiHandler.performSwitchover(leader, replica))
        .thenReturn(Uni.createFrom().voidItem());

    switchoverHandler.performSwitchover(replica.getName(), TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME)
        .await().indefinitely();

    verify(patroniApiHandler).getClusterMembers(TEST_CLUSTER_NAME, TEST_NAMESPACE_NAME);

    verify(patroniApiHandler, never()).performSwitchover(any(), any());

  }

}
