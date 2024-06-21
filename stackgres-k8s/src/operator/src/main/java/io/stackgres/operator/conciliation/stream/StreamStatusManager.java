/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.stackgres.common.StreamUtil;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.crd.sgstream.StreamStatusCondition;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StreamStatusManager
    extends ConditionUpdater<StackGresStream, Condition>
    implements StatusManager<StackGresStream, Condition> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamStatusManager.class);

  private final ResourceFinder<Job> jobFinder;

  @Inject
  public StreamStatusManager(ResourceFinder<Job> jobFinder) {
    this.jobFinder = jobFinder;
  }

  private static String getStreamId(StackGresStream stream) {
    return stream.getMetadata().getNamespace() + "/" + stream.getMetadata().getName();
  }

  @Override
  public StackGresStream refreshCondition(StackGresStream source) {
    final boolean isJobFinishedAndStatusNotUpdated;
    if (Optional.of(source)
        .map(StackGresStream::getStatus)
        .map(StackGresStreamStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .filter(condition -> Objects.equals(condition.getType(),
            StreamStatusCondition.Type.COMPLETED.getType())
            || Objects.equals(condition.getType(),
                StreamStatusCondition.Type.FAILED.getType()))
        .anyMatch(condition -> Objects.equals(condition.getStatus(), "True"))) {
      isJobFinishedAndStatusNotUpdated = false;
    } else {
      final Optional<Job> job = Optional.ofNullable(source.getSpec().getMaxRetries())
          .filter(maxRetries -> maxRetries >= 0)
          .flatMap(maxRetries -> jobFinder.findByNameAndNamespace(
              StreamUtil.jobName(source),
              source.getMetadata().getNamespace()));
      isJobFinishedAndStatusNotUpdated = job
          .map(Job::getStatus)
          .map(JobStatus::getConditions)
          .stream()
          .flatMap(List::stream)
          .filter(condition -> Objects.equals(condition.getType(), "Failed")
              || Objects.equals(condition.getType(), "Completed"))
          .anyMatch(condition -> Objects.equals(condition.getStatus(), "True"));
      if (source.getStatus() == null) {
        source.setStatus(new StackGresStreamStatus());
      }
    }

    if (isJobFinishedAndStatusNotUpdated) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Stream {} failed since the job failed but status condition is neither completed or failed",
            getStreamId(source));
      }
      updateCondition(getFalseRunning(), source);
      updateCondition(getFalseCompleted(), source);
      updateCondition(getFailedDueToUnexpectedFailure(), source);
    }
    return source;
  }

  protected Condition getFalseRunning() {
    return StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition();
  }

  protected Condition getFalseCompleted() {
    return StreamStatusCondition.STREAM_FALSE_COMPLETED.getCondition();
  }

  protected Condition getFailedDueToUnexpectedFailure() {
    var failed = StreamStatusCondition.STREAM_FAILED.getCondition();
    failed.setMessage("Unexpected failure");
    return failed;
  }

  @Override
  protected List<Condition> getConditions(StackGresStream context) {
    return Optional.ofNullable(context.getStatus())
        .map(StackGresStreamStatus::getConditions)
        .orElse(List.of());
  }

  @Override
  protected void setConditions(StackGresStream context, List<Condition> conditions) {
    if (context.getStatus() == null) {
      context.setStatus(new StackGresStreamStatus());
    }
    context.getStatus().setConditions(conditions);
  }

}
