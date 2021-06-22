/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.jobs.dbops.lock.LockAcquirerImpl;
import io.stackgres.jobs.dbops.lock.LockRequest;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.jobs.dbops.securityupgrade.SecurityUpgradeJob;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

@QuarkusTest
class DbOpLauncherImplTest {

  @InjectMock
  @DatabaseOperation("securityUpgrade")
  SecurityUpgradeJob securityUpgradeJob;

  @Inject
  DbOpLauncherImpl dbOpLauncher;

  @Inject
  MockKubeDb mockKubeDb;

  @InjectSpy
  LockAcquirerImpl lockAcquirer;

  StackGresDbOps dbOps;

  StackGresCluster cluster;

  String namespace;
  String randomCluster;
  String randomDbOpsName;

  @BeforeEach
  void setUp() {
    namespace = StringUtils.getRandomNamespace();
    randomDbOpsName = StringUtils.getRandomString();
    randomCluster = StringUtils.getRandomClusterName();

    dbOps = JsonUtil.readFromJson("stackgres_dbops/dbops_securityupgrade.json",
        StackGresDbOps.class);

    cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);

    dbOps.getMetadata().setNamespace(namespace);
    dbOps.getMetadata().setName(randomDbOpsName);
    dbOps.getSpec().setSgCluster(randomCluster);
    dbOps = mockKubeDb.addOrReplaceDbOps(dbOps);

    cluster.getMetadata().setNamespace(namespace);
    cluster.getMetadata().setName(randomCluster);
    cluster = mockKubeDb.addOrReplaceCluster(cluster);

  }

  @Test
  void givenAValidDbOps_shouldExecuteTheJob() {

    when(securityUpgradeJob.runJob(any(StackGresDbOps.class), any(StackGresCluster.class)))
        .thenAnswer(invocation -> Uni.createFrom().item(mockKubeDb.getDbOps(randomDbOpsName, namespace)));

    dbOpLauncher.launchDbOp(randomDbOpsName, namespace);
    verify(securityUpgradeJob).runJob(any(StackGresDbOps.class), any(StackGresCluster.class));

  }

  @Test
  void launchJob_shouldAcquireTheLockBeforeExecutingTheJob(){
    doAnswer((Answer<Void>) invocationOnMock -> null)
        .when(lockAcquirer).lockRun(any(LockRequest.class), any());

    dbOpLauncher.launchDbOp(randomDbOpsName, namespace);
    verify(securityUpgradeJob, never()).runJob(any(StackGresDbOps.class), any(StackGresCluster.class));

  }

  @Test
  void givenAFailureToAcquireLock_itShouldReportTheFailure() {
    final String errorMessage = "lock failure";
    doThrow(new RuntimeException(errorMessage))
        .when(lockAcquirer).lockRun(any(), any());

    assertThrows(RuntimeException.class, () -> dbOpLauncher.launchDbOp(randomDbOpsName, namespace));
  }


  @Test
  void givenAValidDbOps_shouldUpdateItsStatusInformation() {
    when(securityUpgradeJob.runJob(any(StackGresDbOps.class), any(StackGresCluster.class)))
        .thenAnswer(invocation -> Uni.createFrom().item(mockKubeDb.getDbOps(randomDbOpsName, namespace)));

    int initialRetries = Optional.ofNullable(dbOps.getStatus())
        .map(StackGresDbOpsStatus::getOpRetries)
        .orElse(0);
    Instant beforeExecute = Instant.now();

    dbOpLauncher.launchDbOp(randomDbOpsName, namespace);

    var persistedDbOps = mockKubeDb.getDbOps(randomDbOpsName, namespace);
    assertNotNull(persistedDbOps.getStatus(), "DbOpLaucher should initialize the DbOps status");
    assertTrue(persistedDbOps.getStatus().isOpStartedValid(), "opStarted should be a valid date");
    assertTrue(() -> {
      var afterExecute = Instant.now();
      var persistedOpStarted = Instant.parse(persistedDbOps.getStatus().getOpStarted());
      return beforeExecute.isBefore(persistedOpStarted) && afterExecute.isAfter(persistedOpStarted);
    }, "OpStarted should be close to now");
    assertEquals(initialRetries, persistedDbOps.getStatus().getOpRetries());
  }

  @Test
  void givenANonExistentDbOps_shouldThrowIAE() {
    when(securityUpgradeJob.runJob(any(StackGresDbOps.class), any(StackGresCluster.class)))
        .thenAnswer(invocation -> Uni.createFrom().item(mockKubeDb.getDbOps(randomDbOpsName, namespace)));
    assertThrows(IllegalArgumentException.class, () -> dbOpLauncher
        .launchDbOp(StringUtils.getRandomString(), namespace));
  }

  @Test
  void givenAInvalidOp_shouldThrowIEE() {
    when(securityUpgradeJob.runJob(any(StackGresDbOps.class), any(StackGresCluster.class)))
        .thenAnswer(invocation -> Uni.createFrom().item(mockKubeDb.getDbOps(randomDbOpsName, namespace)));
    dbOps.getSpec().setOp(StringUtils.getRandomString());

    dbOps = mockKubeDb.addOrReplaceDbOps(dbOps);
    assertThrows(IllegalStateException.class, () -> dbOpLauncher
        .launchDbOp(randomDbOpsName, namespace));
  }

  @Test
  void givenAValidDbOps_shouldSetRunningConditionsBeforeExecutingTheJob() {
    ArgumentCaptor<StackGresDbOps> captor = ArgumentCaptor.forClass(StackGresDbOps.class);

    when(securityUpgradeJob.runJob(captor.capture(), any(StackGresCluster.class)))
        .thenAnswer(invocation -> {
          return Uni.createFrom().item(mockKubeDb.getDbOps(randomDbOpsName, namespace));
        });

    dbOpLauncher.launchDbOp(randomDbOpsName, namespace);

    StackGresDbOps captured = captor.getValue();

    var conditions = captured.getStatus().getConditions();
    assertNotNull(conditions);
    assertEquals(3, conditions.size());
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_RUNNING::isCondition)
    );
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_COMPLETED::isCondition)
    );
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_FAILED::isCondition)
    );
  }

  @Test
  void givenAValidDbOps_shouldSetCompletedConditionsAfterExecutingTheJob() {
    when(securityUpgradeJob.runJob(any(StackGresDbOps.class), any(StackGresCluster.class)))
        .thenAnswer(invocation -> Uni.createFrom().item((StackGresDbOps) invocation.getArgument(0)));

    dbOpLauncher.launchDbOp(randomDbOpsName, namespace);

    var storedDbOp = mockKubeDb.getDbOps(randomDbOpsName, namespace);
    var conditions = storedDbOp.getStatus().getConditions();
    assertNotNull(conditions);
    assertEquals(3, conditions.size());
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_RUNNING::isCondition)
    );
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_COMPLETED::isCondition)
    );
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_FAILED::isCondition)
    );
  }

  @Test
  void givenAValidDbOps_shouldSetFailedConditionsIdTheJobFails() {
    when(securityUpgradeJob.runJob(any(StackGresDbOps.class), any(StackGresCluster.class)))
        .thenThrow(new RuntimeException("failed job"));

    assertThrows(RuntimeException.class, () -> dbOpLauncher.launchDbOp(randomDbOpsName, namespace));

    var storedDbOp = mockKubeDb.getDbOps(randomDbOpsName, namespace);
    var conditions = storedDbOp.getStatus().getConditions();
    assertNotNull(conditions);
    assertEquals(3, conditions.size());
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_RUNNING::isCondition)
    );
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_COMPLETED::isCondition)
    );
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FAILED::isCondition)
    );
  }


}