/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobConditionBuilder;
import io.stackgres.common.crd.sgshardeddbops.ShardedDbOpsStatusCondition;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsRestartStatus;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.Condition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedDbOpsStatusManagerTest {

  private StackGresShardedDbOps expectedDbOps;
  private StackGresShardedDbOps dbOps;

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

  private ShardedDbOpsStatusManager statusManager;

  @BeforeEach
  void setUp() {
    statusManager = new ShardedDbOpsStatusManager(jobFinder);
    expectedDbOps = Fixtures.shardedDbOps().loadRestart().get();
    dbOps = Fixtures.shardedDbOps().loadRestart().get();
  }

  @Test
  void completedDbOps_shouldNotUpdateResource() {
    dbOps.setStatus(new StackGresShardedDbOpsStatus());
    dbOps.getStatus().setConditions(List.of(
        ShardedDbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        ShardedDbOpsStatusCondition.DBOPS_COMPLETED.getCondition(),
        ShardedDbOpsStatusCondition.DBOPS_FALSE_FAILED.getCondition()));
    expectedDbOps.setStatus(new StackGresShardedDbOpsStatus());
    expectedDbOps.getStatus().setConditions(List.of(
        ShardedDbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        ShardedDbOpsStatusCondition.DBOPS_COMPLETED.getCondition(),
        ShardedDbOpsStatusCondition.DBOPS_FALSE_FAILED.getCondition()));

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(expectedDbOps, dbOps);
    verify(jobFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void failedDbOps_shouldNotUpdateResource() {
    dbOps.setStatus(new StackGresShardedDbOpsStatus());
    dbOps.getStatus().setConditions(List.of(
        ShardedDbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        ShardedDbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        ShardedDbOpsStatusCondition.DBOPS_FAILED.getCondition()));
    expectedDbOps.setStatus(new StackGresShardedDbOpsStatus());
    expectedDbOps.getStatus().setConditions(List.of(
        ShardedDbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        ShardedDbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        ShardedDbOpsStatusCondition.DBOPS_FAILED.getCondition()));

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(expectedDbOps, dbOps);
    verify(jobFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void noJob_shouldNotUpdateResource() {
    expectedDbOps.setStatus(new StackGresShardedDbOpsStatus());
    expectedDbOps.getStatus().setOpRetries(0);

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(expectedDbOps, dbOps);
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void runningJob_shouldNotUpdateResource() {
    expectedDbOps.setStatus(new StackGresShardedDbOpsStatus());
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
        ShardedDbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        ShardedDbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        ShardedDbOpsStatusCondition.DBOPS_FAILED.getCondition(),
        dbOps.getStatus().getConditions(),
        "Unexpected failure");
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void failedJob_shouldUpdateResource() {
    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(failedJob));

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(0, dbOps.getStatus().getOpRetries());
    assertCondition(
        ShardedDbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        ShardedDbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        ShardedDbOpsStatusCondition.DBOPS_FAILED.getCondition(),
        dbOps.getStatus().getConditions(),
        "Unexpected failure");
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void completedJobWithRunningDbOps_shouldUpdateResource() {
    dbOps.setStatus(new StackGresShardedDbOpsStatus());
    dbOps.getStatus().setConditions(List.of(
        ShardedDbOpsStatusCondition.DBOPS_RUNNING.getCondition(),
        ShardedDbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        ShardedDbOpsStatusCondition.DBOPS_FALSE_FAILED.getCondition()));
    Condition.setTransitionTimes(dbOps.getStatus().getConditions());
    dbOps.getStatus().setRestart(new StackGresShardedDbOpsRestartStatus());
    dbOps.getStatus().setOpRetries(0);
    dbOps.getStatus().setOpStarted(Instant.now().toString());

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(completedJob));

    statusManager.refreshCondition(dbOps);

    Assertions.assertEquals(0, dbOps.getStatus().getOpRetries());
    Assertions.assertNotNull(dbOps.getStatus().getOpStarted());
    Assertions.assertNotNull(dbOps.getStatus().getRestart());
    assertCondition(
        ShardedDbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        ShardedDbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        dbOps.getStatus().getConditions());
    assertCondition(
        ShardedDbOpsStatusCondition.DBOPS_FAILED.getCondition(),
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
    var condition = foundConditions.get(0);
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
