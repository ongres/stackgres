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
import io.stackgres.common.crd.sgstream.StreamStatusCondition;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.stream.app.StreamProperty;
import io.stackgres.stream.jobs.lock.LockAcquirer;
import io.stackgres.stream.jobs.lock.LockRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
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
  @Any
  Instance<StreamJob> instance;

  @Inject
  StreamEventEmitter streamEventEmitter;

  @Inject
  StreamExecutorService executorService;

  public void launchStream(String streamName, String namespace) {
    StackGresStream stream = streamFinder.findByNameAndNamespace(streamName, namespace)
        .orElseThrow(() -> new IllegalArgumentException(StackGresStream.KIND + " "
            + streamName + " does not exists in namespace " + namespace));

    Instance<StreamJob> jobImpl =
        instance.select(new StreamTargetOperationLiteral(stream.getSpec().getTarget().getType()));

    if (jobImpl.isResolvable()) {
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
                    .completionStage(jobImpl.get().runJob(initializedStream)))
                .invoke(() -> streamEventEmitter
                    .streamCompleted(streamName, namespace)))
            .runSubscriptionOn(executorService.getExecutorService());
        streamUni.await().indefinitely();

        LOGGER.info("Operation completed for SGStream {}", streamName);
        updateToCompletedConditions(streamName, namespace);
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
    } else if (jobImpl.isAmbiguous()) {
      throw new IllegalStateException("Multiple implementations of the stream target type "
          + stream.getSpec().getTarget().getType() + " found");
    } else {
      throw new IllegalStateException("Implementation of stream target type "
          + stream.getSpec().getTarget().getType()
          + " not found");
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

  private void updateToCompletedConditions(String streamName, String namespace) {
    updateToConditions(streamName, namespace, getCompletedConditions());
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

  public List<Condition> getCompletedConditions() {
    final List<Condition> conditions = List.of(
        StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition(),
        StreamStatusCondition.STREAM_COMPLETED.getCondition(),
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
