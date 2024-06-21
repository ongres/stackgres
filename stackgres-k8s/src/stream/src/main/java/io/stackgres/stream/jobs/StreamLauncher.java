/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import static io.stackgres.stream.app.StreamProperty.STREAM_LOCK_DURATION;
import static io.stackgres.stream.app.StreamProperty.STREAM_LOCK_POLL_INTERVAL;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.crd.sgstream.StreamStatusCondition;
import io.stackgres.common.crd.sgstream.StreamTargetType;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.stream.app.StreamProperty;
import io.stackgres.stream.jobs.lock.LockAcquirer;
import io.stackgres.stream.jobs.lock.LockRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StreamLauncher {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamLauncher.class);

  @Inject
  CustomResourceFinder<StackGresStream> streamFinder;

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  CustomResourceScheduler<StackGresStream> streamScheduler;

  @Inject
  LockAcquirer lockAcquirer;

  @Inject
  StreamJob streamJob;

  @Inject
  @Any
  Instance<SourceEventHandler> sourceEventHandlerInstance;

  @Inject
  @Any
  Instance<TargetEventHandler> targetEventHandlerInstance;

  @Inject
  StreamEventEmitter streamEventEmitter;

  @Inject
  StreamExecutorService executorService;

  public void launchStream(String streamName, String namespace) {
    StackGresStream stream = streamFinder.findByNameAndNamespace(streamName, namespace)
        .orElseThrow(() -> new IllegalArgumentException(StackGresStream.KIND + " "
            + streamName + " does not exists in namespace " + namespace));

    if (Optional.of(stream)
        .map(StackGresStream::getStatus)
        .map(StackGresStreamStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .anyMatch(StreamStatusCondition.STREAM_COMPLETED::isCondition)) {
      LOGGER.info("SGStream already completed, waiting for user to delete the SGStream");
      while (true) {
        Unchecked.runnable(() -> Thread.sleep(1000)).run();
      }
    }
    LOGGER.info("Initializing conditions for SGStream {}", stream.getMetadata().getName());
    final StackGresStream initializedStream = streamScheduler.update(stream,
        (currentStream) -> {
          var status = Optional.ofNullable(currentStream.getStatus())
              .or(() -> Optional.of(new StackGresStreamStatus()))
              .map(streamStatus -> {
                streamStatus.setConditions(getStartingConditions());
                return streamStatus;
              })
              .orElseThrow();
          currentStream.setStatus(status);
        });

    final Instance<SourceEventHandler> sourceEventHandlerImpl =
        sourceEventHandlerInstance.select(new StreamSourceOperationLiteral(
            StreamSourceType.fromString(stream.getSpec().getSource().getType())));

    if (sourceEventHandlerImpl.isAmbiguous()) {
      throw new IllegalStateException("Multiple implementations of the stream source type "
          + stream.getSpec().getSource().getType() + " found");
    } else if (!sourceEventHandlerImpl.isResolvable()) {
      throw new IllegalStateException("Implementation of stream source type "
          + stream.getSpec().getSource().getType()
          + " not found");
    }

    final Instance<TargetEventHandler> targetEventHandlerImpl =
        targetEventHandlerInstance.select(new StreamTargetOperationLiteral(
            StreamTargetType.fromString(stream.getSpec().getTarget().getType())));

    if (targetEventHandlerImpl.isAmbiguous()) {
      throw new IllegalStateException("Multiple implementations of the stream target type "
          + stream.getSpec().getTarget().getType() + " found");
    } else if (!targetEventHandlerImpl.isResolvable()) {
      throw new IllegalStateException("Implementation of stream target type "
          + stream.getSpec().getTarget().getType()
          + " not found");
    }

    try {
      final int lockPollInterval = Integer.parseInt(STREAM_LOCK_POLL_INTERVAL.getString());
      final int duration = Integer.parseInt(STREAM_LOCK_DURATION.getString());

      LockRequest lockRequest = LockRequest.builder()
          .namespace(initializedStream.getMetadata().getNamespace())
          .serviceAccount(StreamProperty.SERVICE_ACCOUNT.getString())
          .podName(StreamProperty.POD_NAME.getString())
          .pollInterval(lockPollInterval)
          .duration(duration)
          .lockResourceName(initializedStream.getMetadata().getName())
          .build();

      Infrastructure.setDroppedExceptionHandler(err -> LOGGER.error("Dropped exception ", err));

      var streamUni =
          lockAcquirer.lockRun(lockRequest, Uni.createFrom().voidItem()
              .invoke(() -> streamEventEmitter
                  .streamStarted(streamName, namespace))
              .chain(() -> Uni.createFrom()
                  .completionStage(streamJob.runJob(
                      initializedStream,
                      sourceEventHandlerImpl.get(),
                      targetEventHandlerImpl.get())))
              .invoke(() -> streamEventEmitter
                  .streamCompleted(streamName, namespace))
              .invoke(() -> {
                LOGGER.info("Operation completed for SGStream {}", streamName);
                if (Optional.ofNullable(stream.getSpec().getMaxRetries()).orElse(-1) < 0) {
                  LOGGER.info("SGStream completed, waiting for user to delete the SGStream");
                  while (true) {
                    Unchecked.runnable(() -> Thread.sleep(1000)).run();
                  }
                }
              })
              )
          .runSubscriptionOn(executorService.getExecutorService());
      streamUni.await().indefinitely();
    } catch (TimeoutException timeoutEx) {
      updateToTimeoutConditions(streamName, namespace);
      streamEventEmitter.streamTimedOut(streamName, namespace);
      throw timeoutEx;
    } catch (Exception e) {
      LOGGER.info("Unexpected exception for SGStream {}", streamName, e);
      updateToFailedConditions(streamName, namespace);
      streamEventEmitter.streamFailed(streamName, namespace);
      throw e;
    }
  }

  private void updateToConditions(String streamName, String namespace, List<Condition> conditions) {
    Uni.createFrom()
        .item(() -> streamFinder.findByNameAndNamespace(streamName, namespace)
            .orElseThrow())
        .invoke(currentStream -> currentStream.getStatus().setConditions(conditions))
        .invoke(streamScheduler::update)
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("updating conditions for SGStream"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .indefinitely()
        .await().indefinitely();
  }

  private void updateToFailedConditions(String streamName, String namespace) {
    updateToConditions(streamName, namespace, getFailedConditions());
  }

  private void updateToTimeoutConditions(String streamName, String namespace) {
    updateToConditions(streamName, namespace, getTimeoutConditions());
  }

  public List<Condition> getStartingConditions() {
    final List<Condition> conditions = List.of(
        StreamStatusCondition.STREAM_RUNNING.getCondition(),
        StreamStatusCondition.STREAM_FALSE_COMPLETED.getCondition(),
        StreamStatusCondition.STREAM_FALSE_FAILED.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    return conditions;
  }

  public List<Condition> getFailedConditions() {
    final List<Condition> conditions = List.of(
        StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition(),
        StreamStatusCondition.STREAM_FALSE_COMPLETED.getCondition(),
        StreamStatusCondition.STREAM_FAILED.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    return conditions;
  }

  public List<Condition> getTimeoutConditions() {
    final List<Condition> conditions = List.of(
        StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition(),
        StreamStatusCondition.STREAM_FALSE_COMPLETED.getCondition(),
        StreamStatusCondition.STREAM_TIMED_OUT.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    return conditions;
  }

}
