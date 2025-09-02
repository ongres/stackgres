/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobConditionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
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

  private StackGresDbOps expectedDbOps;
  private StackGresDbOps dbOps;

  private final Job runningJob = new JobBuilder()
      .withNewStatus()
      .withConditions(
          new JobConditionBuilder()
          .withType("Completed")
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
          .withType("Completed")
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
          .withType("Completed")
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
  PatroniCtl patroniCtl;

  private DbOpsStatusManager statusManager;

  @BeforeEach
  void setUp() {
    statusManager = new DbOpsStatusManager(jobFinder, clusterFinder,
        labelFactory, statefulSetFinder, podScanner, patroniCtl);
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
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        DbOpsStatusCondition.DBOPS_FAILED.getCondition().setLastTransitionTime()));

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(completedJob));

    statusManager.refreshCondition(dbOps);

    assertCondition(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        DbOpsStatusCondition.DBOPS_COMPLETED.getCondition(),
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
    assertCondition(
        DbOpsStatusCondition.DBOPS_FAILED.getCondition(),
        dbOps.getStatus().getConditions(),
        "Unexpected failure");
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
        DbOpsStatusCondition.DBOPS_COMPLETED.getCondition(),
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
        DbOpsStatusCondition.DBOPS_FAILED.getCondition(),
        dbOps.getStatus().getConditions(),
        "Unexpected failure");
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
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
