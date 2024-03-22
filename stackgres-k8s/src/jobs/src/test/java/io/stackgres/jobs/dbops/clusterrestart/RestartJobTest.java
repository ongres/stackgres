/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.jobs.dbops.DatabaseOperation;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RestartJobTest {

  private final AtomicInteger clusterNr = new AtomicInteger(0);
  @Inject
  @DatabaseOperation("restart")
  RestartJob restartJob;
  @Inject
  MockKubeDb kubeDb;

  @InjectMock
  @StateHandler("restart")
  ClusterRestartStateHandler clusterRestart;

  private StackGresCluster cluster;
  private StackGresDbOps dbOps;
  private String clusterName;
  private String clusterNamespace;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().setName("test-" + clusterNr.incrementAndGet());
    clusterName = StringUtils.getRandomResourceName();
    clusterNamespace = StringUtils.getRandomNamespace();
    cluster.getMetadata().setName(clusterName);
    cluster.getMetadata().setNamespace(clusterNamespace);

    dbOps = Fixtures.dbOps().loadRestart().get();
    dbOps.getMetadata().setNamespace(clusterNamespace);
    dbOps.getMetadata().setName(clusterName);
    dbOps.getSpec().setSgCluster(clusterName);
  }

  @AfterEach
  void tearDown() {
    kubeDb.delete(cluster);
    kubeDb.delete(dbOps);
  }

  @Test
  void restartJob_shouldRestartTheCluster() {
    doReturn(Uni.createFrom().voidItem())
        .when(clusterRestart).restartCluster(any());

    cluster = kubeDb.addOrReplaceCluster(cluster);

    restartJob.runJob(dbOps, cluster).await().indefinitely();

    verify(clusterRestart).restartCluster(any());
  }

  @Test
  void givenAFailureToRestartTheCluster_itShouldReportTheFailure() {
    final String errorMessage = "restart failure";
    doReturn(Uni.createFrom().failure(new RuntimeException(errorMessage)))
        .when(clusterRestart).restartCluster(any());

    cluster = kubeDb.addOrReplaceCluster(cluster);
    dbOps = kubeDb.addOrReplaceDbOps(dbOps);

    assertThrows(RuntimeException.class,
        () -> restartJob.runJob(dbOps, cluster).await().indefinitely());

    assertEquals(errorMessage, kubeDb.getDbOps(clusterName, clusterNamespace)
        .getStatus().getRestart().getFailure());
  }
}
