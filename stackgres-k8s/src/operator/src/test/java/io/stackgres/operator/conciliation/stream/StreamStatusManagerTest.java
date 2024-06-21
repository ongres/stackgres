/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobConditionBuilder;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.crd.sgstream.StreamStatusCondition;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.Condition;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamStatusManagerTest {

  private StackGresStream expectedStream;
  private StackGresStream stream;

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

  private StreamStatusManager statusManager;

  @BeforeEach
  void setUp() {
    statusManager = new StreamStatusManager(jobFinder);
    expectedStream = Fixtures.stream().loadSgClusterToCloudEvent().get();
    expectedStream.getSpec().setMaxRetries(0);
    stream = JsonUtil.copy(expectedStream);
  }

  @Test
  void completedStream_shouldNotUpdateResource() {
    stream.setStatus(new StackGresStreamStatus());
    stream.getStatus().setConditions(List.of(
        StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition(),
        StreamStatusCondition.STREAM_COMPLETED.getCondition(),
        StreamStatusCondition.STREAM_FALSE_FAILED.getCondition()));
    expectedStream.setStatus(new StackGresStreamStatus());
    expectedStream.getStatus().setConditions(List.of(
        StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition(),
        StreamStatusCondition.STREAM_COMPLETED.getCondition(),
        StreamStatusCondition.STREAM_FALSE_FAILED.getCondition()));

    statusManager.refreshCondition(stream);

    Assertions.assertEquals(expectedStream, stream);
    verify(jobFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void failedStream_shouldNotUpdateResource() {
    stream.setStatus(new StackGresStreamStatus());
    stream.getStatus().setConditions(List.of(
        StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition(),
        StreamStatusCondition.STREAM_FALSE_COMPLETED.getCondition(),
        StreamStatusCondition.STREAM_FAILED.getCondition()));
    expectedStream.setStatus(new StackGresStreamStatus());
    expectedStream.getStatus().setConditions(List.of(
        StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition(),
        StreamStatusCondition.STREAM_FALSE_COMPLETED.getCondition(),
        StreamStatusCondition.STREAM_FAILED.getCondition()));

    statusManager.refreshCondition(stream);

    Assertions.assertEquals(expectedStream, stream);
    verify(jobFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void noJob_shouldNotUpdateResource() {
    expectedStream.setStatus(new StackGresStreamStatus());

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());

    statusManager.refreshCondition(stream);

    Assertions.assertEquals(expectedStream, stream);
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void runningJob_shouldNotUpdateResource() {
    expectedStream.setStatus(new StackGresStreamStatus());

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(runningJob));

    statusManager.refreshCondition(stream);

    Assertions.assertEquals(expectedStream, stream);
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void completedJob_shouldUpdateResource() {
    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(completedJob));

    statusManager.refreshCondition(stream);

    assertCondition(
        StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition(),
        stream.getStatus().getConditions());
    assertCondition(
        StreamStatusCondition.STREAM_FALSE_COMPLETED.getCondition(),
        stream.getStatus().getConditions());
    assertCondition(
        StreamStatusCondition.STREAM_FAILED.getCondition(),
        stream.getStatus().getConditions(),
        "Unexpected failure");
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void failedJob_shouldUpdateResource() {
    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(failedJob));

    statusManager.refreshCondition(stream);

    assertCondition(
        StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition(),
        stream.getStatus().getConditions());
    assertCondition(
        StreamStatusCondition.STREAM_FALSE_COMPLETED.getCondition(),
        stream.getStatus().getConditions());
    assertCondition(
        StreamStatusCondition.STREAM_FAILED.getCondition(),
        stream.getStatus().getConditions(),
        "Unexpected failure");
    verify(jobFinder, times(1)).findByNameAndNamespace(any(), any());
  }

  @Test
  void completedJobWithRunningStream_shouldUpdateResource() {
    stream.setStatus(new StackGresStreamStatus());
    stream.getStatus().setConditions(List.of(
        StreamStatusCondition.STREAM_RUNNING.getCondition(),
        StreamStatusCondition.STREAM_FALSE_COMPLETED.getCondition(),
        StreamStatusCondition.STREAM_FALSE_FAILED.getCondition()));
    Condition.setTransitionTimes(stream.getStatus().getConditions());

    when(jobFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(completedJob));

    statusManager.refreshCondition(stream);

    assertCondition(
        StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition(),
        stream.getStatus().getConditions());
    assertCondition(
        StreamStatusCondition.STREAM_FALSE_COMPLETED.getCondition(),
        stream.getStatus().getConditions());
    assertCondition(
        StreamStatusCondition.STREAM_FAILED.getCondition(),
        stream.getStatus().getConditions(),
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
