/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.patroni.PatroniCtl.PatroniCtlInstance;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class PatroniApiHandlerTest {

  @InjectMock
  PatroniCtlFinder patroniCtlFinder;

  @Inject
  PatroniApiHandler patroniApiHandler;

  PatroniCtlInstance patroniCtl = Mockito.mock(PatroniCtlInstance.class);

  String clusterName = StringUtils.getRandomString();
  String namespace = StringUtils.getRandomString();

  private void preparePatroniMetadata() {
    when(patroniCtlFinder.findPatroniCtl(any(), any()))
        .thenReturn(patroniCtl);
  }

  @Test
  void givenValidCredentials_shouldRetrieveClusterMembers() {
    preparePatroniMetadata();

    patroniApiHandler.getClusterMembers(clusterName, namespace)
        .await()
        .atMost(Duration.ofSeconds(5));
  }

  @Test
  void patroniInformation_shouldNotFail() {
    preparePatroniMetadata();

    patroniApiHandler
        .getClusterMembersPatroniInformation(clusterName, namespace)
        .await()
        .atMost(Duration.ofSeconds(5));
  }

  @Test
  void givenValidCredentials_shouldPerformSwitchOver() {
    preparePatroniMetadata();

    when(patroniCtlFinder.getPatroniCredentials(any(), any()))
        .thenReturn(Tuple.tuple("test", "test"));

    PatroniMember leader = new PatroniMember();
    leader.setCluster(clusterName);
    leader.setMember("leader-member");
    leader.setState(PatroniMember.RUNNING);
    leader.setRole(PatroniMember.LEADER);
    PatroniMember replica = new PatroniMember();
    replica.setCluster(clusterName);
    replica.setMember("replica-member");
    replica.setState(PatroniMember.RUNNING);
    replica.setRole(PatroniMember.REPLICA);

    patroniApiHandler.performSwitchover(clusterName, namespace, leader, replica)
        .await()
        .atMost(Duration.ofSeconds(5));
  }

  @Test
  void givenValidCredentials_shouldRestartPostgres() {
    preparePatroniMetadata();

    when(patroniCtlFinder.getPatroniCredentials(any(), any()))
        .thenReturn(Tuple.tuple("test", "test"));

    PatroniMember leader = new PatroniMember();
    leader.setCluster(clusterName);
    leader.setMember("leader-member");
    leader.setState(PatroniMember.RUNNING);
    leader.setRole(PatroniMember.LEADER);

    patroniApiHandler.restartPostgres(clusterName, namespace, leader)
        .await()
        .atMost(Duration.ofSeconds(5));
  }

}
