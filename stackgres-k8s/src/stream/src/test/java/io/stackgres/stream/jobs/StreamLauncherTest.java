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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
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
import io.stackgres.stream.configuration.CustomPrometheusMeterRegistry;
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
  @StreamTargetOperation(StreamTargetType.CLOUD_EVENT)
  TargetEventHandler targetEventHandler;

  @InjectMock
  @StreamSourceOperation(StreamSourceType.SGCLUSTER)
  SourceEventHandler clusterEventHandler;

  @Inject
  StreamLauncher streamLauncher;

  @Inject
  MockKubeDb mockKubeDb;

  @InjectSpy
  LockAcquirer lockAcquirer;

  @InjectMock
  StreamEventEmitter streamEventEmitter;

  @Inject
  MeterRegistry meterRegistry;

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
    stream.getSpec().setMaxRetries(0);

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

  private CompletableFuture<Void> getCompletedCompletableFuture() {
    return CompletableFuture.completedFuture(null);
  }

  @Test
  void givenAValidStream_shouldExecuteTheJob() {
    when(targetEventHandler.sendEvents(any(), any()))
        .thenAnswer(invocation -> getCompletedCompletableFuture());
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
    verify(targetEventHandler, never()).sendEvents(any(StackGresStream.class), any());

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
    when(targetEventHandler.sendEvents(any(), any()))
        .thenAnswer(invocation -> getCompletedCompletableFuture());

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
    when(targetEventHandler.sendEvents(any(), any()))
        .thenAnswer(invocation -> getCompletedCompletableFuture());
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
    when(targetEventHandler.sendEvents(any(), any()))
        .thenAnswer(invocation -> getCompletedCompletableFuture());
    String targetType = StringUtils.getRandomString();
    stream.getSpec().getTarget().setType(targetType);

    stream = mockKubeDb.addOrReplaceStream(stream);
    var ex = assertThrows(IllegalArgumentException.class, () -> streamLauncher
        .launchStream(randomStreamName, namespace));

    assertEquals("SGStream target type " + targetType + " is invalid",
        ex.getMessage());

    verify(streamEventEmitter, never()).streamStarted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamCompleted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamTimedOut(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamFailed(randomStreamName, namespace);
  }

  @Test
  void givenAInvalidSourceType_shouldThrowIllegalStateException() {
    when(targetEventHandler.sendEvents(any(), any()))
        .thenAnswer(invocation -> getCompletedCompletableFuture());
    String sourceType = StringUtils.getRandomString();
    stream.getSpec().getSource().setType(sourceType);

    stream = mockKubeDb.addOrReplaceStream(stream);
    var ex = assertThrows(IllegalArgumentException.class, () -> streamLauncher
        .launchStream(randomStreamName, namespace));

    assertEquals("SGStream source type " + sourceType + " is invalid",
        ex.getMessage());

    verify(streamEventEmitter, never()).streamStarted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamCompleted(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamTimedOut(randomStreamName, namespace);
    verify(streamEventEmitter, never()).streamFailed(randomStreamName, namespace);
  }

  @Test
  void givenAValidStream_shouldSetRunningConditionsBeforeExecutingTheJob() {
    ArgumentCaptor<StackGresStream> captor = ArgumentCaptor.forClass(StackGresStream.class);

    when(targetEventHandler.sendEvents(captor.capture(), any()))
        .thenAnswer(invocation -> getCompletedCompletableFuture());

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
  void givenAValidStream_shouldNotSetCompletedConditionsAfterExecutingTheJobIfTombstoneWasNotSent() {
    when(targetEventHandler.sendEvents(any(), any()))
        .thenAnswer(invocation -> getCompletedCompletableFuture());

    streamLauncher.launchStream(randomStreamName, namespace);

    var storedDbOp = mockKubeDb.getStream(randomStreamName, namespace);
    var conditions = storedDbOp.getStatus().getConditions();
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
  void givenAValidStream_shouldSetFailedConditionsIdTheJobFails() {
    when(targetEventHandler.sendEvents(any(), any()))
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

    when(targetEventHandler.sendEvents(captor.capture(), any()))
        .thenAnswer(invocation -> getCompletedCompletableFuture());

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

  @Test
  void checkMeterRegistry() {
    CompositeMeterRegistry compositeMeterRegistry = (CompositeMeterRegistry) meterRegistry;
    compositeMeterRegistry.getRegistries().forEach(registry -> System.out.println(registry.getClass().getName()));
    assertEquals(1, compositeMeterRegistry.getRegistries().size());
    assertEquals(
        CustomPrometheusMeterRegistry.class,
        compositeMeterRegistry.getRegistries().iterator().next().getClass());
  }

}
