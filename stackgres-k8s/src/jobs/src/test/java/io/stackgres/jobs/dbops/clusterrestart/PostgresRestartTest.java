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

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
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

  List<ClusterMember> clusterMembers;

  PatroniInformation patroniInformation;

  @BeforeEach
  void setUp() {
    clusterName = StringUtils.getRandomClusterName();
    namespace = StringUtils.getRandomNamespace();
    memberName = StringUtils.getRandomClusterName();

    clusterMembers = List.of(
        ImmutableClusterMember.builder()
            .name(memberName)
            .clusterName(clusterName)
            .namespace(namespace)
            .state(MemberState.RUNNING)
            .role(MemberRole.LEADER)
            .host("127.0.0.1")
            .apiUrl("http://127.0.0.1:8008/patroni")
            .port(7433)
            .timeline(1)
            .build(),
        ImmutableClusterMember.builder()
            .name(StringUtils.getRandomString())
            .clusterName(clusterName)
            .namespace(namespace)
            .state(MemberState.RUNNING)
            .role(MemberRole.REPLICA)
            .host("127.0.0.2")
            .apiUrl("http://127.0.0.2:8008/patroni")
            .port(7433)
            .timeline(1)
            .lag(0)
            .build());

    patroniInformation = ImmutablePatroniInformation.builder()
        .state(MemberState.RUNNING)
        .role(MemberRole.REPLICA)
        .patroniScope(clusterName)
        .patroniVersion("2.1.2")
        .serverVersion(142000)
        .isPendingRestart(true)
        .build();
  }

  @Test
  void restartPostgres_shouldNotFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.get(0)))
        .thenReturn(Uni.createFrom().item(patroniInformation));

    postgresRestart.restartPostgres(memberName, clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(1)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(1)).restartPostgres(any());
  }

  @Test
  void restartPostgresWhenNotPendingRestart_shouldNotFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.get(0)))
        .thenReturn(Uni.createFrom().item(ImmutablePatroniInformation
            .copyOf(patroniInformation)
            .withIsPendingRestart(false)));

    postgresRestart.restartPostgresWithoutRetry(memberName, clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(1)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(0)).restartPostgres(any());
  }

  @Test
  void restartPostgresWhenStarting_shouldNotFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.get(0)))
        .thenReturn(Uni.createFrom().item(ImmutablePatroniInformation
            .copyOf(patroniInformation)
            .withState(MemberState.STARTING)))
        .thenReturn(Uni.createFrom().item(ImmutablePatroniInformation
            .copyOf(patroniInformation)
            .withIsPendingRestart(false)));

    postgresRestart.restartPostgresWithoutRetry(memberName, clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(2)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(0)).restartPostgres(any());
  }

  @Test
  void restartPostgresWhenAlreadyRestarting_shouldNotFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.get(0)))
        .thenReturn(Uni.createFrom().item(ImmutablePatroniInformation
            .copyOf(patroniInformation)
            .withState(MemberState.RESTARTING)))
        .thenReturn(Uni.createFrom().item(ImmutablePatroniInformation
            .copyOf(patroniInformation)
            .withIsPendingRestart(false)));

    postgresRestart.restartPostgresWithoutRetry(memberName, clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(2)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(0)).restartPostgres(any());
  }

  @Test
  void restartPostgresWhenAlreadyRestarting_shouldFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.get(0)))
        .thenReturn(Uni.createFrom().item(patroniInformation));
    when(patroniApiHandler.restartPostgres(clusterMembers.get(0)))
        .thenReturn(Uni.createFrom()
            .failure(() -> new RuntimeException("status 503: null")));

    assertThrows(Exception.class, () -> postgresRestart
        .restartPostgresWithoutRetry(memberName, clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50)));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(2)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(1)).restartPostgres(any());
  }

  @Test
  void givenANonExistentMember_shouldFail() {
    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
    when(patroniApiHandler.getClusterMemberPatroniInformation(clusterMembers.get(0)))
        .thenReturn(Uni.createFrom().item(patroniInformation));

    assertThrows(Exception.class, () -> postgresRestart
        .restartPostgresWithoutRetry(StringUtils.getRandomString(), clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50)));

    verify(patroniApiHandler, times(1)).getClusterMembers(any(), any());
    verify(patroniApiHandler, times(0)).getClusterMemberPatroniInformation(any());
    verify(patroniApiHandler, times(0)).restartPostgres(any());
  }
}
