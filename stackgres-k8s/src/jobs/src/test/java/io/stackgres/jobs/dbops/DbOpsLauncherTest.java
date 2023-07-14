/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsOperation;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import io.stackgres.jobs.dbops.lock.LockAcquirer;
import io.stackgres.jobs.dbops.lock.LockRequest;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.jobs.dbops.securityupgrade.SecurityUpgradeJob;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.stubbing.Answer;

@QuarkusTest
class DbOpsLauncherTest {

  @InjectMock
  @DatabaseOperation("securityUpgrade")
  SecurityUpgradeJob securityUpgradeJob;

  @Inject
  DbOpsLauncher dbOpLauncher;

  @Inject
  MockKubeDb mockKubeDb;

  @InjectSpy
  LockAcquirer lockAcquirer;

  @InjectMock
  DatabaseOperationEventEmitter databaseOperationEventEmitter;

  StackGresDbOps dbOps;

  StackGresCluster cluster;

  String namespace;
  String randomClusterName;
  String randomDbOpsName;

  @BeforeEach
  void setUp() {
    namespace = StringUtils.getRandomNamespace();
    randomDbOpsName = StringUtils.getRandomString();
    randomClusterName = StringUtils.getRandomClusterName();

    dbOps = Fixtures.dbOps().loadSecurityUpgrade().get();

    cluster = Fixtures.cluster().loadDefault().get();

    dbOps.getMetadata().setNamespace(namespace);
    dbOps.getMetadata().setName(randomDbOpsName);
    dbOps.getSpec().setSgCluster(randomClusterName);
    dbOps = mockKubeDb.addOrReplaceDbOps(dbOps);

    cluster.getMetadata().setNamespace(namespace);
    cluster.getMetadata().setName(randomClusterName);
    cluster = mockKubeDb.addOrReplaceCluster(cluster);
  }

  private Uni<ClusterRestartState> getClusterRestartStateUni() {
    Pod primary = new Pod();
    return Uni.createFrom().item(
        ClusterRestartState.builder()
            .namespace(dbOps.getMetadata().getNamespace())
            .dbOpsName(dbOps.getMetadata().getName())
            .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
            .clusterName(dbOps.getSpec().getSgCluster())
            .isOnlyPendingRestart(false)
            .isSwitchoverInitiated(false)
            .isSwitchoverFinalized(false)
            .restartMethod(DbOpsMethodType.IN_PLACE)
            .primaryInstance(primary)
            .initialInstances(ImmutableList.of(primary))
            .totalInstances(ImmutableList.of(primary))
            .podRestartReasonsMap(ImmutableMap.of(primary, RestartReasons.of()))
            .build());
  }

  @Test
  void givenAValidDbOps_shouldExecuteTheJob() {
    when(securityUpgradeJob.runJob(any(), any()))
        .thenAnswer(invocation -> getClusterRestartStateUni());
    doNothing().when(databaseOperationEventEmitter).operationStarted(randomDbOpsName, namespace);
    doNothing().when(databaseOperationEventEmitter).operationCompleted(randomDbOpsName, namespace);

    dbOpLauncher.launchDbOp(randomDbOpsName, namespace);

    final InOrder inOrder = inOrder(databaseOperationEventEmitter);
    inOrder.verify(databaseOperationEventEmitter).operationStarted(randomDbOpsName, namespace);
    inOrder.verify(databaseOperationEventEmitter).operationCompleted(randomDbOpsName, namespace);
  }

  @Test
  void launchJob_shouldAcquireTheLockBeforeExecutingTheJob() {
    doAnswer((Answer<Uni<?>>) invocationOnMock -> Uni.createFrom().voidItem())
        .when(lockAcquirer).lockRun(any(LockRequest.class), any());

    dbOpLauncher.launchDbOp(randomDbOpsName, namespace);
    verify(securityUpgradeJob, never()).runJob(any(StackGresDbOps.class),
        any(StackGresCluster.class));

    verify(databaseOperationEventEmitter, never()).operationStarted(randomDbOpsName, namespace);
    verify(databaseOperationEventEmitter, never()).operationCompleted(randomDbOpsName, namespace);
    verify(databaseOperationEventEmitter, never()).operationFailed(randomDbOpsName, namespace);
    verify(databaseOperationEventEmitter, never()).operationTimedOut(randomDbOpsName, namespace);

  }

  @Test
  void givenAFailureToAcquireLock_itShouldReportTheFailure() {
    final String errorMessage = "lock failure";
    doThrow(new RuntimeException(errorMessage))
        .when(lockAcquirer).lockRun(any(), any());
    doNothing().when(databaseOperationEventEmitter).operationFailed(randomDbOpsName, namespace);

    assertThrows(RuntimeException.class, () -> dbOpLauncher.launchDbOp(randomDbOpsName, namespace));

    verify(databaseOperationEventEmitter, never()).operationStarted(randomDbOpsName, namespace);
    verify(databaseOperationEventEmitter, never()).operationCompleted(randomDbOpsName, namespace);
    verify(databaseOperationEventEmitter, never()).operationTimedOut(randomDbOpsName, namespace);
    verify(databaseOperationEventEmitter).operationFailed(randomDbOpsName, namespace);
  }

  @Test
  void givenATimeout_itShouldReportTheTimeout() {
    when(securityUpgradeJob.runJob(any(), any()))
        .thenAnswer(invocation -> getClusterRestartStateUni()
            .invoke(Unchecked.consumer(item -> Thread.sleep(10000))));
    doNothing().when(databaseOperationEventEmitter).operationStarted(randomDbOpsName, namespace);
    doNothing().when(databaseOperationEventEmitter).operationCompleted(randomDbOpsName, namespace);

    dbOps.getSpec().setTimeout(Duration.ofMillis(10).toString());
    assertThrows(TimeoutException.class, () -> dbOpLauncher.launchDbOp(randomDbOpsName, namespace));

    verify(databaseOperationEventEmitter, atMost(1)).operationStarted(randomDbOpsName, namespace);
    verify(databaseOperationEventEmitter, atMost(1)).operationCompleted(randomDbOpsName, namespace);
    verify(databaseOperationEventEmitter).operationTimedOut(randomDbOpsName, namespace);
    verify(databaseOperationEventEmitter, never()).operationFailed(randomDbOpsName, namespace);
  }

  @Test
  void givenAValidDbOps_shouldUpdateItsStatusInformation() {
    when(securityUpgradeJob.runJob(any(), any()))
        .thenAnswer(invocation -> getClusterRestartStateUni());

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
    assertNull(persistedDbOps.getStatus().getOpRetries());
  }

  @Test
  void givenANonExistentDbOps_shouldThrowIllegalArgumentException() {
    when(securityUpgradeJob.runJob(any(), any()))
        .thenAnswer(invocation -> getClusterRestartStateUni());
    assertThrows(IllegalArgumentException.class, () -> dbOpLauncher
        .launchDbOp(StringUtils.getRandomString(), namespace));
  }

  @Test
  void givenAInvalidOp_shouldThrowIllegalStateException() {
    when(securityUpgradeJob.runJob(any(), any()))
        .thenAnswer(invocation -> getClusterRestartStateUni());
    dbOps.getSpec().setOp(StringUtils.getRandomString());

    dbOps = mockKubeDb.addOrReplaceDbOps(dbOps);
    assertThrows(IllegalStateException.class, () -> dbOpLauncher
        .launchDbOp(randomDbOpsName, namespace));
  }

  @Test
  void givenAValidDbOps_shouldSetRunningConditionsBeforeExecutingTheJob() {
    ArgumentCaptor<StackGresDbOps> captor = ArgumentCaptor.forClass(StackGresDbOps.class);

    when(securityUpgradeJob.runJob(captor.capture(), any()))
        .thenAnswer(invocation -> getClusterRestartStateUni());

    dbOpLauncher.launchDbOp(randomDbOpsName, namespace);

    StackGresDbOps captured = captor.getValue();

    assertNotNull(captured.getStatus().getOpStarted());
    assertTrue(Instant.parse(captured.getStatus().getOpStarted()).isBefore(Instant.now()));
    assertNull(captured.getStatus().getOpRetries());
    var conditions = captured.getStatus().getConditions();
    assertNotNull(conditions);
    assertEquals(3, conditions.size());
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_RUNNING::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_COMPLETED::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_FAILED::isCondition));
  }

  @Test
  void givenAValidDbOps_shouldSetCompletedConditionsAfterExecutingTheJob() {
    when(securityUpgradeJob.runJob(any(), any()))
        .thenAnswer(invocation -> getClusterRestartStateUni());

    dbOpLauncher.launchDbOp(randomDbOpsName, namespace);

    var storedDbOp = mockKubeDb.getDbOps(randomDbOpsName, namespace);
    assertNotNull(storedDbOp.getStatus().getOpStarted());
    assertTrue(Instant.parse(storedDbOp.getStatus().getOpStarted()).isBefore(Instant.now()));
    assertNull(storedDbOp.getStatus().getOpRetries());
    var conditions = storedDbOp.getStatus().getConditions();
    assertNotNull(conditions);
    assertEquals(3, conditions.size());
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_RUNNING::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_COMPLETED::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_FAILED::isCondition));
  }

  @Test
  void givenAValidDbOps_shouldSetFailedConditionsIdTheJobFails() {
    when(securityUpgradeJob.runJob(any(), any()))
        .thenThrow(new RuntimeException("failed job"));

    assertThrows(RuntimeException.class, () -> dbOpLauncher.launchDbOp(randomDbOpsName, namespace));

    var storedDbOp = mockKubeDb.getDbOps(randomDbOpsName, namespace);
    assertNotNull(storedDbOp.getStatus().getOpStarted());
    assertTrue(Instant.parse(storedDbOp.getStatus().getOpStarted()).isBefore(Instant.now()));
    assertNull(storedDbOp.getStatus().getOpRetries());
    var conditions = storedDbOp.getStatus().getConditions();
    assertNotNull(conditions);
    assertEquals(3, conditions.size());
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_RUNNING::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_COMPLETED::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FAILED::isCondition));
  }

  @Test
  void givenAValidDbOpsRetry_shouldSetRunningConditionsBeforeExecutingTheJob() {
    ArgumentCaptor<StackGresDbOps> captor = ArgumentCaptor.forClass(StackGresDbOps.class);

    when(securityUpgradeJob.runJob(captor.capture(), any()))
        .thenAnswer(invocation -> getClusterRestartStateUni());

    Instant previousOpStarted = Instant.now();
    dbOps.setStatus(new StackGresDbOpsStatus());
    dbOps.getStatus().setOpStarted(previousOpStarted.toString());
    dbOps.getStatus().setOpRetries(0);
    dbOps.getStatus().setConditions(Seq.of(
        DbOpsStatusCondition.DB_OPS_FALSE_RUNNING,
        DbOpsStatusCondition.DB_OPS_FALSE_COMPLETED,
        DbOpsStatusCondition.DB_OPS_FAILED)
        .map(DbOpsStatusCondition::getCondition)
        .peek(condition -> condition.setLastTransitionTime(previousOpStarted.toString()))
        .toList());
    mockKubeDb.addOrReplaceDbOps(dbOps);

    dbOpLauncher.launchDbOp(randomDbOpsName, namespace);

    StackGresDbOps captured = captor.getValue();

    assertNotNull(captured.getStatus().getOpStarted());
    assertTrue(Instant.parse(captured.getStatus().getOpStarted()).isBefore(Instant.now()));
    assertEquals(0, captured.getStatus().getOpRetries());
    var conditions = captured.getStatus().getConditions();
    assertNotNull(conditions);
    assertEquals(3, conditions.size());
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_RUNNING::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_COMPLETED::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(DbOpsStatusCondition.DB_OPS_FALSE_FAILED::isCondition));
  }

}
