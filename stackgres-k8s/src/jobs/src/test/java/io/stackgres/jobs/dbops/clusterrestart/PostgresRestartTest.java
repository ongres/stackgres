/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PostgresRestartTest {

  @Inject
  PostgresRestart postgresRestart;

  @InjectMock
  PatroniApiHandler patroniApiHandler;

  String clusterName;

  String namespace;

  String memberName;

  List<PatroniMember> clusterMembers;

  PatroniInformation patroniInformation;

  @BeforeEach
  void setUp() {
    clusterName = StringUtils.getRandomResourceName();
    namespace = StringUtils.getRandomNamespace();
    memberName = StringUtils.getRandomResourceName();

    var leader = new PatroniMember();
    leader.setMember(memberName);
    leader.setCluster(clusterName);
    leader.setState(PatroniMember.RUNNING);
    leader.setRole(PatroniMember.LEADER);
    leader.setTimeline("1");
    var replica = new PatroniMember();
    replica.setMember(StringUtils.getRandomString());
    replica.setCluster(clusterName);
    replica.setState(PatroniMember.RUNNING);
    replica.setRole(PatroniMember.REPLICA);
    replica.setTimeline("1");
    replica.setLagInMb(new IntOrString(0));
    clusterMembers = List.of(leader, replica);

    patroniInformation = ImmutablePatroniInformation.builder()
        .state(PatroniMember.MemberState.RUNNING)
        .role(PatroniMember.MemberRole.REPLICA)
        .isPendingRestart(true)
        .build();
  }

  @Test
  void restartPostgres_shouldNotFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.getFirst()))
        .thenReturn(patroniInformation);

    postgresRestart.restartPostgres(memberName, clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(1)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(1)).restartPostgres(any(), any(), any());
  }

  @Test
  void restartPostgresWhenNotPendingRestart_shouldNotFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.getFirst()))
        .thenReturn(ImmutablePatroniInformation
            .copyOf(patroniInformation)
            .withIsPendingRestart(false));

    postgresRestart.restartPostgresWithoutRetry(memberName, clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(1)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(0)).restartPostgres(any(), any(), any());
  }

  @Test
  void restartPostgresWhenStarting_shouldNotFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.getFirst()))
        .thenReturn(ImmutablePatroniInformation
            .copyOf(patroniInformation)
            .withState(PatroniMember.MemberState.STARTING))
        .thenReturn(ImmutablePatroniInformation
            .copyOf(patroniInformation)
            .withIsPendingRestart(false));

    postgresRestart.restartPostgresWithoutRetry(memberName, clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(2)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(0)).restartPostgres(any(), any(), any());
  }

  @Test
  void restartPostgresWhenAlreadyRestarting_shouldNotFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.getFirst()))
        .thenReturn(ImmutablePatroniInformation
            .copyOf(patroniInformation)
            .withState(PatroniMember.MemberState.RESTARTING))
        .thenReturn(ImmutablePatroniInformation
            .copyOf(patroniInformation)
            .withIsPendingRestart(false));

    postgresRestart.restartPostgresWithoutRetry(memberName, clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(2)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(0)).restartPostgres(any(), any(), any());
  }

  @Test
  void restartPostgresWhenAlreadyRestarting_shouldFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.getFirst()))
        .thenReturn(patroniInformation);
    when(patroniApiHandler.restartPostgres(clusterName, namespace, clusterMembers.getFirst()))
        .thenReturn(Uni.createFrom()
            .failure(() -> new RuntimeException("status 503: null")));

    assertThrows(Exception.class, () -> postgresRestart
        .restartPostgresWithoutRetry(memberName, clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50)));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(2)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(1)).restartPostgres(any(), any(), any());
  }

  @Test
  void givenANonExistentMember_shouldFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.getFirst()))
        .thenReturn(patroniInformation);

    assertThrows(Exception.class, () -> postgresRestart
        .restartPostgresWithoutRetry(StringUtils.getRandomString(), clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50)));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(0)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(0)).restartPostgres(any(), any(), any());
  }
}
