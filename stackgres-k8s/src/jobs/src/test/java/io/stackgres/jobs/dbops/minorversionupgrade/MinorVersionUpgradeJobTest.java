/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.minorversionupgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.jobs.dbops.DatabaseOperation;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MinorVersionUpgradeJobTest {

  private final AtomicInteger clusterNr = new AtomicInteger(0);
  @Inject
  @DatabaseOperation("minorVersionUpgrade")
  MinorVersionUpgradeJob minorVerionUpgradeJob;
  @Inject
  MockKubeDb kubeDb;

  @InjectMock
  @StateHandler("minorVersionUpgrade")
  MinorVersionUpgradeRestartStateHandlerImpl clusterRestart;

  private StackGresCluster cluster;
  private StackGresDbOps dbOps;
  private String clusterName;
  private String clusterNamespace;

  @BeforeEach
  void setUp() {
    cluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
    cluster.getMetadata().setName("test-" + clusterNr.incrementAndGet());
    clusterName = StringUtils.getRandomClusterName();
    clusterNamespace = StringUtils.getRandomNamespace();
    cluster.getMetadata().setName(clusterName);
    cluster.getMetadata().setNamespace(clusterNamespace);

    dbOps =
        JsonUtil.readFromJson("stackgres_dbops/dbops_minorversionupgrade.json",
            StackGresDbOps.class);
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
  void minorVersionUpgradeJob_shouldUpdateThePostgresVersionOfTheTargetCluster() {
    final String expectedPotgresVersion = dbOps.getSpec().getMinorVersionUpgrade()
        .getPostgresVersion();
    cluster = kubeDb.addOrReplaceCluster(cluster);
    minorVerionUpgradeJob.runJob(dbOps, cluster).await().indefinitely();
    var storedClusterPostgresVersion = kubeDb.getCluster(clusterName, clusterNamespace)
        .getSpec().getPostgres().getVersion();
    assertEquals(expectedPotgresVersion, storedClusterPostgresVersion);
  }

  @Test
  void minorVersionUpgradeJob_shouldRestartTheCluster() {
    doReturn(Uni.createFrom().voidItem()).when(clusterRestart).restartCluster(any());

    cluster = kubeDb.addOrReplaceCluster(cluster);

    minorVerionUpgradeJob.runJob(dbOps, cluster).await().indefinitely();

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
        () -> minorVerionUpgradeJob.runJob(dbOps, cluster).await().indefinitely());

    assertEquals(errorMessage, kubeDb.getDbOps(clusterName, clusterNamespace)
        .getStatus().getMinorVersionUpgrade().getFailure());
  }
}
