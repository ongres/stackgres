/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.crd.sgstream.StreamStatusCondition;
import io.stackgres.common.crd.sgstream.StreamTargetType;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.stream.jobs.cloudevent.StreamCloudEventJob;
import io.stackgres.stream.jobs.cloudevent.StreamEventState;
import io.stackgres.stream.jobs.lock.LockAcquirer;
import io.stackgres.stream.jobs.lock.LockRequest;
import io.stackgres.stream.jobs.lock.MockKubeDb;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.stubbing.Answer;

@QuarkusTest
class StreamLauncherTest {

  @InjectMock
  @StreamTargetOperation("CloudEvent")
  StreamCloudEventJob streamCloudEventJob;

  @Inject
  StreamLauncher streamLauncher;

  @Inject
  MockKubeDb mockKubeDb;

  @InjectSpy
  LockAcquirer lockAcquirer;

  @InjectMock
  StreamEventEmitter streamEventEmitter;

  StackGresStream stream;

  StackGresCluster cluster;

  String namespace;
  String randomClusterName;
  String randomStreamName;

  @BeforeEach
  void setUp() {
    namespace = StringUtils.getRandomNamespace();
    randomStreamName = StringUtils.getRandomString();
    randomClusterName = StringUtils.getRandomResourceName();

    stream = Fixtures.stream().loadSgClusterToCloudEvent().get();

    cluster = Fixtures.cluster().loadDefault().get();

    stream.getMetadata().setNamespace(namespace);
    stream.getMetadata().setName(randomStreamName);
    stream.getSpec().getSource().getSgCluster().setName(randomClusterName);
    stream = mockKubeDb.addOrReplaceStream(stream);

    cluster.getMetadata().setNamespace(namespace);
    cluster.getMetadata().setName(randomClusterName);
    cluster = mockKubeDb.addOrReplaceCluster(cluster);

    doNothing().when(streamEventEmitter).streamStarted(randomStreamName, namespace);
    doNothing().when(streamEventEmitter).streamFailed(randomStreamName, namespace);
    doNothing().when(streamEventEmitter).streamCompleted(randomStreamName, namespace);
    doNothing().when(streamEventEmitter).streamTimedOut(randomStreamName, namespace);
  }

  private CompletableFuture<StreamEventState> getStreamEventStateCompletableFuture() {
    Pod primary = new Pod();
    primary.setMetadata(new ObjectMeta());
    primary.getMetadata().setName(stream.getMetadata().getName() + "-0");
    return CompletableFuture.completedFuture(
        StreamEventState.builder()
            .namespace(stream.getMetadata().getNamespace())
            .streamName(stream.getMetadata().getName())
            .streamOperation(new StreamTargetOperationLiteral(stream.getSpec().getTarget().getType()))
            .sourceType(StreamSourceType.fromString(stream.getSpec().getSource().getType()))
            .targetType(StreamTargetType.fromString(stream.getSpec().getTarget().getType()))
            .build());
  }

  @Test
  void givenAValidStream_shouldExecuteTheJob() {
    when(streamCloudEventJob.runJob(any()))
        .thenAnswer(invocation -> getStreamEventStateCompletableFuture());
    streamLauncher.launchStream(randomStreamName, namespace);

    final InOrder inOrder = inOrder(streamEventEmitter);
    inOrder.verify(streamEventEmitter).streamStarted(randomStreamName, namespace);
    inOrder.verify(streamEventEmitter).streamCompleted(randomStreamName, namespace);
  }

  @Test
  void launchJob_shouldAcquireTheLockBeforeExecutingTheJob() {
    doAnswer((Answer<Uni<?>>) invocationOnMock -> Uni.createFrom().voidItem())
        .when(lockAcquirer).lockRun(any(LockRequest.class), any());

    streamLauncher.launchStream(randomStreamName, namespace);
    verify(streamCloudEventJob, never()).runJob(any(StackGresStream.class));

    verify(streamEventEmitter, never()).streamStarted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamCompleted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamFailed(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamTimedOut(randomStreamName, namespace);
  }

  @Test
  void givenAFailureToAcquireLock_itShouldReportTheFailure() {
    final String errorMessage = "lock failure";
    doThrow(new RuntimeException(errorMessage))
        .when(lockAcquirer).lockRun(any(), any());
    doNothing().when(streamEventEmitter).streamFailed(randomStreamName, namespace);

    assertThrows(RuntimeException.class, () -> streamLauncher.launchStream(randomStreamName, namespace));

    verify(streamEventEmitter, never()).streamStarted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamCompleted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamTimedOut(randomStreamName, namespace);
    verify(streamEventEmitter).streamFailed(randomStreamName, namespace);
  }

  @Test
  void givenAValidStream_shouldUpdateItsStatusInformation() {
    when(streamCloudEventJob.runJob(any()))
        .thenAnswer(invocation -> getStreamEventStateCompletableFuture());

    streamLauncher.launchStream(randomStreamName, namespace);

    var persistedStream = mockKubeDb.getStream(randomStreamName, namespace);
    assertNotNull(persistedStream.getStatus(), "DbOpLaucher should initialize the Stream status");
    verify(streamEventEmitter, times(1)).streamStarted(randomStreamName, namespace);
    verify(streamEventEmitter, times(1)).streamCompleted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamTimedOut(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamFailed(randomStreamName, namespace);
  }

  @Test
  void givenANonExistentStream_shouldThrowIllegalArgumentException() {
    when(streamCloudEventJob.runJob(any()))
        .thenAnswer(invocation -> getStreamEventStateCompletableFuture());
    String streamName = StringUtils.getRandomString();
    var ex = assertThrows(IllegalArgumentException.class, () -> streamLauncher
        .launchStream(streamName, namespace));

    assertEquals("SGStream " + streamName + " does not exists in namespace " + namespace,
        ex.getMessage());

    verify(streamEventEmitter, never()).streamStarted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamCompleted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamTimedOut(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamFailed(randomStreamName, namespace);
  }

  @Test
  void givenAInvalidTargetType_shouldThrowIllegalStateException() {
    when(streamCloudEventJob.runJob(any()))
        .thenAnswer(invocation -> getStreamEventStateCompletableFuture());
    String targetType = StringUtils.getRandomString();
    stream.getSpec().getTarget().setType(targetType);

    stream = mockKubeDb.addOrReplaceStream(stream);
    var ex = assertThrows(IllegalStateException.class, () -> streamLauncher
        .launchStream(randomStreamName, namespace));

    assertEquals("Implementation of stream target type " + targetType + " not found", ex.getMessage());

    verify(streamEventEmitter, never()).streamStarted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamCompleted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamTimedOut(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamFailed(randomStreamName, namespace);
  }

  @Test
  void givenAValidStream_shouldSetRunningConditionsBeforeExecutingTheJob() {
    ArgumentCaptor<StackGresStream> captor = ArgumentCaptor.forClass(StackGresStream.class);

    when(streamCloudEventJob.runJob(captor.capture()))
        .thenAnswer(invocation -> getStreamEventStateCompletableFuture());

    streamLauncher.launchStream(randomStreamName, namespace);

    StackGresStream captured = captor.getValue();

    var conditions = captured.getStatus().getConditions();
    assertNotNull(conditions);
    assertEquals(3, conditions.size());
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_RUNNING::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_FALSE_COMPLETED::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_FALSE_FAILED::isCondition));

    verify(streamEventEmitter, times(1)).streamStarted(randomStreamName, namespace);
    verify(streamEventEmitter, times(1)).streamCompleted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamTimedOut(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamFailed(randomStreamName, namespace);
  }

  @Test
  void givenAValidStream_shouldSetCompletedConditionsAfterExecutingTheJob() {
    when(streamCloudEventJob.runJob(any()))
        .thenAnswer(invocation -> getStreamEventStateCompletableFuture());

    streamLauncher.launchStream(randomStreamName, namespace);

    var storedDbOp = mockKubeDb.getStream(randomStreamName, namespace);
    var conditions = storedDbOp.getStatus().getConditions();
    assertNotNull(conditions);
    assertEquals(3, conditions.size());
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_FALSE_RUNNING::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_COMPLETED::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_FALSE_FAILED::isCondition));

    verify(streamEventEmitter, times(1)).streamStarted(randomStreamName, namespace);
    verify(streamEventEmitter, times(1)).streamCompleted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamTimedOut(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamFailed(randomStreamName, namespace);
  }

  @Test
  void givenAValidStream_shouldSetFailedConditionsIdTheJobFails() {
    when(streamCloudEventJob.runJob(any()))
        .thenThrow(new RuntimeException("failed job"));

    assertThrows(RuntimeException.class, () -> streamLauncher.launchStream(randomStreamName, namespace));

    var storedDbOp = mockKubeDb.getStream(randomStreamName, namespace);
    var conditions = storedDbOp.getStatus().getConditions();
    assertNotNull(conditions);
    assertEquals(3, conditions.size());
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_FALSE_RUNNING::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_FALSE_COMPLETED::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_FAILED::isCondition));

    verify(streamEventEmitter, times(1)).streamStarted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamCompleted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamTimedOut(randomStreamName, namespace);
    verify(streamEventEmitter, times(1)).streamFailed(randomStreamName, namespace);
  }

  @Test
  void givenAValidStreamRetry_shouldSetRunningConditionsBeforeExecutingTheJob() {
    ArgumentCaptor<StackGresStream> captor = ArgumentCaptor.forClass(StackGresStream.class);

    when(streamCloudEventJob.runJob(captor.capture()))
        .thenAnswer(invocation -> getStreamEventStateCompletableFuture());

    Instant previousOpStarted = Instant.now();
    stream.setStatus(new StackGresStreamStatus());
    stream.getStatus().setConditions(Seq.of(
        StreamStatusCondition.STREAM_FALSE_RUNNING,
        StreamStatusCondition.STREAM_FALSE_COMPLETED,
        StreamStatusCondition.STREAM_FAILED)
        .map(StreamStatusCondition::getCondition)
        .peek(condition -> condition.setLastTransitionTime(previousOpStarted.toString()))
        .toList());
    mockKubeDb.addOrReplaceStream(stream);

    streamLauncher.launchStream(randomStreamName, namespace);

    StackGresStream captured = captor.getValue();

    var conditions = captured.getStatus().getConditions();
    assertNotNull(conditions);
    assertEquals(3, conditions.size());
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_RUNNING::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_FALSE_COMPLETED::isCondition));
    assertTrue(() -> conditions.stream()
        .anyMatch(StreamStatusCondition.STREAM_FALSE_FAILED::isCondition));

    verify(streamEventEmitter, times(1)).streamStarted(randomStreamName, namespace);
    verify(streamEventEmitter, times(1)).streamCompleted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamTimedOut(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamFailed(randomStreamName, namespace);
  }

}
