/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodConditionBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobConditionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniCtlInstance;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operatorframework.resource.Condition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsStatusManagerTest {

  private static final String UPDATE_REVISION = "test-7d4b9c8f6d";

  private StackGresDbOps expectedDbOps;
  private StackGresDbOps dbOps;

  private final Job runningJob = new JobBuilder()
      .withNewStatus()
      .withConditions(
          new JobConditionBuilder()
          .withType("Complete")
          .withStatus("False")
          .build(),
          new JobConditionBuilder()
          .withType("Failed")
          .withStatus("False")
          .build())
      .endStatus()
      .build();

  private final Job failedJob = new JobBuilder()
      .withNewStatus()
      .withConditions(
          new JobConditionBuilder()
          .withType("Complete")
          .withStatus("False")
          .build(),
          new JobConditionBuilder()
          .withType("Failed")
          .withStatus("True")
          .build())
      .endStatus()
      .build();

  private final Job completedJob = new JobBuilder()
      .withNewStatus()
      .withConditions(new JobConditionBuilder()
          .withType("Complete")
          .withStatus("True")
          .build(),
          new JobConditionBuilder()
          .withType("Failed")
          .withStatus("False")
          .build())
      .endStatus()
      .build();

  @Mock
  ResourceFinder<Job> jobFinder;

  @Mock
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Mock
  LabelFactoryForCluster labelFactory;

  @Mock
  ResourceFinder<StatefulSet> statefulSetFinder;

  @Mock
  ResourceScanner<Pod> podScanner;

  @Mock
  ResourceFinder<Endpoints> endpointsFinder;

  @Mock
  PatroniCtl patroniCtl;

  @Mock
  PatroniCtlInstance patroniCtlInstance;

  private DbOpsStatusManager statusManager;

  @BeforeEach
  void setUp() {
    statusManager = new DbOpsStatusManager(jobFinder, clusterFinder,
        labelFactory, statefulSetFinder, podScanner, endpointsFinder, patroniCtl);
    expectedDbOps = Fixtures.dbOps().loadPgbench().get();
    dbOps = Fixtures.dbOps().loadPgbench().get();
  }

  @Test
  void completedDbOps_shouldNotUpdateResource() {
    dbOps.setStatus(new StackGresDbOpsStatus());
    dbOps.getStatus().setConditions(List.of(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        DbOpsStatusCondition.DBOPS_COMPLETED.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_FAILED.getCondition()));
    expectedDbOps.setStatus(new StackGresDbOpsStatus());
    expectedDbOps.getStatus().setConditions(List.of(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        DbOpsStatusCondition.DBOPS_COMPLETED.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_FAILED.getCondition()));

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(expectedDbOps, dbOps);
    verify(jobFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void failedDbOps_shouldNotUpdateResource() {
    dbOps.setStatus(new StackGresDbOpsStatus());
    dbOps.getStatus().setConditions(List.of(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        DbOpsStatusCondition.DBOPS_FAILED.getCondition()));
    expectedDbOps.setStatus(new StackGresDbOpsStatus());
    expectedDbOps.getStatus().setConditions(List.of(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        DbOpsStatusCondition.DBOPS_FAILED.getCondition()));
    expectedDbOps.getStatus().setOpRetries(0);

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(expectedDbOps, dbOps);
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void failedDbOpsWithCompletedJob_shouldUpdateResource() {
    dbOps.setStatus(new StackGresDbOpsStatus());
    dbOps.getStatus().setConditions(List.of(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition().setLastTransitionTime(),
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition().setLastTransitionTime(),
        DbOpsStatusCondition.DBOPS_FAILED.getCondition().setLastTransitionTime()));

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(completedJob));

    statusManager.refreshCondition(dbOps);

    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_FAILED.getCondition(),
        dbOps.getStatus().getConditions());
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void noJob_shouldNotUpdateResource() {
    dbOps.setStatus(null);
    expectedDbOps.setStatus(new StackGresDbOpsStatus());
    expectedDbOps.getStatus().setOpRetries(0);

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(expectedDbOps, dbOps);
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void runningJob_shouldNotUpdateResource() {
    dbOps.setStatus(null);
    expectedDbOps.setStatus(new StackGresDbOpsStatus());
    expectedDbOps.getStatus().setOpRetries(0);

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(runningJob));

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(expectedDbOps, dbOps);
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void completedJob_shouldUpdateResource() {
    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(completedJob));

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(0, dbOps.getStatus().getOpRetries());
    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_COMPLETED.getCondition(),
        dbOps.getStatus().getConditions());
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void failedJob_shouldNotUpdateResource() {
    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(failedJob));

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(0, dbOps.getStatus().getOpRetries());
    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_FAILED.getCondition(),
        dbOps.getStatus().getConditions(),
        "Unexpected failure");
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void completedJobWithRunningDbOps_shouldUpdateResource() {
    dbOps.setStatus(new StackGresDbOpsStatus());
    dbOps.getStatus().setConditions(List.of(
        DbOpsStatusCondition.DBOPS_RUNNING.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_FAILED.getCondition()));
    Condition.setTransitionTimes(dbOps.getStatus().getConditions());
    dbOps.getStatus().setOpRetries(0);
    dbOps.getStatus().setOpStarted(Instant.now().toString());

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(completedJob));

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(0, dbOps.getStatus().getOpRetries());
    Assertions.assertNotNull(dbOps.getStatus().getOpStarted());
    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_COMPLETED.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_FAILED.getCondition(),
        dbOps.getStatus().getConditions());
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void majorVersionUpgradeWaitingForRollbackDecision_shouldSetWaitingRollbackCondition() {
    StackGresDbOps mvu = Fixtures.dbOps().loadMajorVersionUpgrade().get();
    mvu.setStatus(new StackGresDbOpsStatus());
    var mvuStatus = new StackGresDbOpsMajorVersionUpgradeStatus();
    mvuStatus.setPhase("wait-post-failed-upgrade-decision");
    mvu.getStatus().setMajorVersionUpgrade(mvuStatus);

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(runningJob));

    statusManager.refreshCondition(mvu);

    assertCondition(
        DbOpsStatusCondition.DBOPS_WAITING_ROLLBACK_AFTER_FAILED.getCondition(),
        mvu.getStatus().getConditions());
  }

  @Test
  void majorVersionUpgradeWithRollbackDecision_shouldNotSetWaitingRollbackCondition() {
    StackGresDbOps mvu = Fixtures.dbOps().loadMajorVersionUpgrade().get();
    mvu.setStatus(new StackGresDbOpsStatus());
    var mvuStatus = new StackGresDbOpsMajorVersionUpgradeStatus();
    mvuStatus.setPhase("wait-post-failed-upgrade-decision");
    mvuStatus.setRollback(false);
    mvu.getStatus().setMajorVersionUpgrade(mvuStatus);

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(runningJob));

    statusManager.refreshCondition(mvu);

    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_WAITING_ROLLBACK.getCondition(),
        mvu.getStatus().getConditions());
  }

  @Test
  void rolloutCompletedWithoutLastUpdate_shouldNotCompleteTheDbOps() {
    StackGresDbOps restart = setUpCompletedRollout();

    statusManager.refreshCondition(restart);

    assertCondition(
        DbOpsStatusCondition.DBOPS_RUNNING.getCondition(),
        restart.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        restart.getStatus().getConditions());
    Assertions.assertNotNull(restart.getStatus().getRestart().getLastUpdate(),
        "The last update of the status has not been set");
  }

  @Test
  void rolloutCompletedWithinTheStatusUpdateDelay_shouldNotCompleteTheDbOps() {
    StackGresDbOps restart = setUpCompletedRollout();
    setLastUpdate(restart, Instant.now().minus(Duration.ofSeconds(10)));

    statusManager.refreshCondition(restart);

    assertCondition(
        DbOpsStatusCondition.DBOPS_RUNNING.getCondition(),
        restart.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        restart.getStatus().getConditions());
  }

  @Test
  void rolloutCompletedAfterTheStatusUpdateDelay_shouldCompleteTheDbOps() {
    StackGresDbOps restart = setUpCompletedRollout();
    setLastUpdate(restart, Instant.now().minus(Duration.ofMinutes(2)));

    statusManager.refreshCondition(restart);

    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        restart.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_ROLLOUT_COMPLETED.getCondition(),
        restart.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_COMPLETED.getCondition(),
        restart.getStatus().getConditions());
  }

  @Test
  void rolloutCompletedAfterACustomStatusUpdateDelay_shouldNotCompleteTheDbOps() {
    StackGresDbOps restart = setUpCompletedRollout();
    restart.getSpec().getRestart().setStatusUpdateDelay("PT10M");
    setLastUpdate(restart, Instant.now().minus(Duration.ofMinutes(2)));

    statusManager.refreshCondition(restart);

    assertCondition(
        DbOpsStatusCondition.DBOPS_RUNNING.getCondition(),
        restart.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        restart.getStatus().getConditions());
  }

  private void setLastUpdate(StackGresDbOps restart, Instant lastUpdate) {
    if (restart.getStatus() == null) {
      restart.setStatus(new StackGresDbOpsStatus());
    }
    restart.getStatus().setRestart(new StackGresDbOpsRestartStatus());
    restart.getStatus().getRestart().setLastUpdate(lastUpdate.toString());
  }

  /**
   * Set up an SGDbOps of type restart targeting a cluster of a single instance which Pod is ready,
   * up to date with the StatefulSet and with no pending restart reported by patroni.
   */
  private StackGresDbOps setUpCompletedRollout() {
    StackGresDbOps restart = Fixtures.dbOps().loadRestart().get();

    StackGresCluster cluster = new StackGresCluster();
    cluster.setMetadata(new ObjectMeta());
    cluster.getMetadata().setName(restart.getSpec().getSgCluster());
    cluster.getMetadata().setNamespace(restart.getMetadata().getNamespace());
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setInstances(1);

    StatefulSet statefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withName(cluster.getMetadata().getName())
        .withNamespace(cluster.getMetadata().getNamespace())
        .endMetadata()
        .withNewStatus()
        .withUpdateRevision(UPDATE_REVISION)
        .endStatus()
        .build();

    Pod pod = new PodBuilder()
        .withNewMetadata()
        .withName(cluster.getMetadata().getName() + "-0")
        .withNamespace(cluster.getMetadata().getNamespace())
        .withLabels(Map.of("controller-revision-hash", UPDATE_REVISION))
        .endMetadata()
        .withNewStatus()
        .withPhase("Running")
        .withConditions(new PodConditionBuilder()
            .withType("Ready")
            .withStatus("True")
            .build())
        .endStatus()
        .build();

    PatroniMember member = new PatroniMember();
    member.setMember(pod.getMetadata().getName());
    member.setRole(PatroniMember.LEADER);
    member.setState(PatroniMember.RUNNING);

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));
    when(statefulSetFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(statefulSet));
    when(labelFactory.clusterLabels(any()))
        .thenReturn(Map.of());
    when(podScanner.getResourcesInNamespaceWithLabels(any(), any()))
        .thenReturn(List.of(pod));
    when(patroniCtl.instanceFor(any()))
        .thenReturn(patroniCtlInstance);
    when(patroniCtlInstance.list())
        .thenReturn(List.of(member));

    return restart;
  }

  private void assertCondition(Condition expectedCondition, List<? extends Condition> conditions) {
    assertCondition(expectedCondition, conditions, null);
  }

  private void assertCondition(Condition expectedCondition, List<? extends Condition> conditions,
      String message) {
    var foundConditions = conditions.stream()
        .filter(condition -> Objects.equals(expectedCondition.getType(), condition.getType()))
        .collect(Collectors.toList());
    Assertions.assertEquals(1, foundConditions.size(),
        "Found more than one condition with type " + expectedCondition.getType());
    var condition = foundConditions.getFirst();
    Assertions.assertEquals(expectedCondition.getStatus(), condition.getStatus(),
        "Condition with type " + expectedCondition.getType() + " has not expected status");
    Assertions.assertEquals(expectedCondition.getReason(), condition.getReason(),
        "Condition with type " + expectedCondition.getType() + " has not expected reason");
    if (message == null) {
      Assertions.assertNull(condition.getMessage(),
          "Condition with type " + expectedCondition.getType() + " has not null message, but was "
          + condition.getMessage());
    } else {
      Assertions.assertEquals(message, condition.getMessage(),
          "Condition with type " + expectedCondition.getType() + " has not expected message");
    }
    Assertions.assertNotNull(condition.getLastTransitionTime(),
        "Condition with type " + expectedCondition.getType() + " has null last transition time");
  }

}
