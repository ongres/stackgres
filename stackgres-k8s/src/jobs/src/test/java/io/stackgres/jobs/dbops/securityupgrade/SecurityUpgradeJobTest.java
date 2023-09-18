/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.StatefulSetFinder;
import io.stackgres.jobs.app.JobsProperty;
import io.stackgres.jobs.dbops.DatabaseOperation;
import io.stackgres.jobs.dbops.JobsStatefulSetWriter;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SecurityUpgradeJobTest {

  private static final String PREVIOUS_OPERATOR_VERSION = "0.9.4";
  private final AtomicInteger clusterNr = new AtomicInteger(0);
  @Inject
  @DatabaseOperation("securityUpgrade")
  SecurityUpgradeJob securityUpgradeJob;
  @Inject
  MockKubeDb kubeDb;

  @InjectMock
  @StateHandler("securityUpgrade")
  SecurityUpgradeStateHandler clusterRestart;

  @InjectMock
  StatefulSetFinder statefulSetReader;
  @InjectMock
  JobsStatefulSetWriter statefulSetWriter;

  private StackGresCluster cluster;
  private StackGresDbOps dbOps;
  private String clusterName;
  private String clusterNamespace;
  private StatefulSet oldStatefulSet;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    oldStatefulSet = Fixtures.statefulSet().load0_9_5().get();
    cluster.getMetadata().setName("test-" + clusterNr.incrementAndGet());
    clusterName = StringUtils.getRandomClusterName();
    clusterNamespace = StringUtils.getRandomNamespace();
    cluster.getMetadata().setName(clusterName);
    cluster.getMetadata().setNamespace(clusterNamespace);
    oldStatefulSet.getMetadata().setName(clusterName);
    oldStatefulSet.getMetadata().setNamespace(clusterNamespace);
    when(statefulSetReader.findByNameAndNamespace(clusterName, clusterNamespace))
        .thenReturn(Optional.of(oldStatefulSet));

    dbOps = Fixtures.dbOps().loadSecurityUpgrade().get();
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
  void upgradeJob_shouldUpdateTheOperatorVersionOfTheTargetCluster() {

    final String expectedOperatorVersion = JobsProperty.OPERATOR_VERSION.getString();
    cluster.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, PREVIOUS_OPERATOR_VERSION);
    cluster = kubeDb.addOrReplaceCluster(cluster);
    securityUpgradeJob.runJob(dbOps, cluster).await().indefinitely();
    var storedClusterVersion = kubeDb.getCluster(clusterName, clusterNamespace)
        .getMetadata().getAnnotations()
        .get(StackGresContext.VERSION_KEY);
    assertEquals(expectedOperatorVersion, storedClusterVersion);

  }

  @Test
  void upgradeJob_shouldDeleteTheExistentStatefulSet() {

    cluster = kubeDb.addOrReplaceCluster(cluster);
    securityUpgradeJob.runJob(dbOps, cluster).await().indefinitely();

    verify(statefulSetReader).findByNameAndNamespace(clusterName, clusterNamespace);
    verify(statefulSetWriter).delete(oldStatefulSet);
  }

  @Test
  void upgradeJob_shouldRestartTheCluster() {

    doReturn(Uni.createFrom().voidItem()).when(clusterRestart).restartCluster(any());

    cluster.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, PREVIOUS_OPERATOR_VERSION);
    cluster = kubeDb.addOrReplaceCluster(cluster);

    securityUpgradeJob.runJob(dbOps, cluster).await().indefinitely();

    verify(clusterRestart).restartCluster(any());

  }

  @Test
  void givenAFailureToRestartTheCluster_itShouldReportTheFailure() {

    final String errorMessage = "restart failure";
    doReturn(Uni.createFrom().failure(new RuntimeException(errorMessage)))
        .when(clusterRestart).restartCluster(any());

    cluster.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, PREVIOUS_OPERATOR_VERSION);
    cluster = kubeDb.addOrReplaceCluster(cluster);
    dbOps = kubeDb.addOrReplaceDbOps(dbOps);

    assertThrows(RuntimeException.class,
        () -> securityUpgradeJob.runJob(dbOps, cluster).await().indefinitely());

    final String expectedOperatorVersion = JobsProperty.OPERATOR_VERSION.getString();

    assertEquals(expectedOperatorVersion, kubeDb.getCluster(clusterName, clusterNamespace)
        .getMetadata().getAnnotations().get(StackGresContext.VERSION_KEY));

    assertEquals(errorMessage, kubeDb.getDbOps(clusterName, clusterNamespace)
        .getStatus().getSecurityUpgrade().getFailure());

  }
}
