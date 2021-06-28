/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PostgresRestartImplTest {

  @Inject
  PostgresRestartImpl postgresRestart;

  @InjectMock
  PatroniApiHandlerImpl patroniApiHandler;

  String clusterName;

  String namespace;

  String memberName;

  List<ClusterMember> clusterMembers;

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
            .role(MemberRole.REPlICA)
            .host("127.0.0.2")
            .apiUrl("http://127.0.0.2:8008/patroni")
            .port(7433)
            .timeline(1)
            .lag(0)
            .build()
    );


    when(patroniApiHandler.getClusterMembers(clusterName, namespace))
        .thenReturn(Uni.createFrom().item(clusterMembers));
  }

  @Test
  void restartPostgres_shouldNotFail() {

    postgresRestart.restartPostgres(memberName, clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50));

  }

  @Test
  void givenANonExistentMember_shouldFail() {

    assertThrows(Exception.class, () -> postgresRestart
        .restartPostgres(StringUtils.getRandomString(), clusterName, namespace)
        .await()
        .atMost(Duration.ofMillis(50)));

  }
}