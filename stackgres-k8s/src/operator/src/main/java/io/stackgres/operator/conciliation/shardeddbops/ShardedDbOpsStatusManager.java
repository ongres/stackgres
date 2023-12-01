/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.stackgres.common.ShardedDbOpsUtil;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgshardeddbops.ShardedDbOpsStatusCondition;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsStatus;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShardedDbOpsStatusManager
    extends ConditionUpdater<StackGresShardedDbOps, Condition>
    implements StatusManager<StackGresShardedDbOps, Condition> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShardedDbOpsStatusManager.class);

  private final ResourceFinder<Job> jobFinder;

  @Inject
  public ShardedDbOpsStatusManager(ResourceFinder<Job> jobFinder) {
    this.jobFinder = jobFinder;
  }

  private static String getShardedDbOpsId(StackGresShardedDbOps dbOps) {
    return dbOps.getMetadata().getNamespace() + "/" + dbOps.getMetadata().getName();
  }

  @Override
  public StackGresShardedDbOps refreshCondition(StackGresShardedDbOps source) {
    final boolean isJobFailedAndStatusNotUpdated;
    if (Optional.of(source)
        .map(StackGresShardedDbOps::getStatus)
        .map(StackGresShardedDbOpsStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .filter(condition -> Objects.equals(condition.getType(),
            ShardedDbOpsStatusCondition.Type.COMPLETED.getType())
            || Objects.equals(condition.getType(),
                ShardedDbOpsStatusCondition.Type.FAILED.getType()))
        .anyMatch(condition -> Objects.equals(condition.getStatus(), "True"))) {
      isJobFailedAndStatusNotUpdated = false;
    } else {
      final Optional<Job> job = jobFinder.findByNameAndNamespace(
          ShardedDbOpsUtil.jobName(source),
          source.getMetadata().getNamespace());
      isJobFailedAndStatusNotUpdated = job
          .map(Job::getStatus)
          .map(JobStatus::getConditions)
          .stream()
          .flatMap(List::stream)
          .filter(condition -> Objects.equals(condition.getType(), "Failed")
              || Objects.equals(condition.getType(), "Completed"))
          .anyMatch(condition -> Objects.equals(condition.getStatus(), "True"));
      if (source.getStatus() == null) {
        source.setStatus(new StackGresShardedDbOpsStatus());
      }
      final int active = job
          .map(Job::getStatus)
          .map(JobStatus::getActive)
          .orElse(0);
      final int failed = job
          .map(Job::getStatus)
          .map(JobStatus::getFailed)
          .orElse(0);
      source.getStatus().setOpRetries(Math.max(0, failed - 1) + (failed > 0 ? active : 0));
    }

    if (isJobFailedAndStatusNotUpdated) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "ShardedDbOps {} failed since the job failed but status condition is neither completed or failed",
            getShardedDbOpsId(source));
      }
      updateCondition(getFalseRunning(), source);
      updateCondition(getFalseCompleted(), source);
      updateCondition(getFailedDueToUnexpectedFailure(), source);
    }
    return source;
  }

  protected Condition getFalseRunning() {
    return ShardedDbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition();
  }

  protected Condition getFalseCompleted() {
    return ShardedDbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition();
  }

  protected Condition getFailedDueToUnexpectedFailure() {
    var failed = ShardedDbOpsStatusCondition.DBOPS_FAILED.getCondition();
    failed.setMessage("Unexpected failure");
    return failed;
  }

  @Override
  protected List<Condition> getConditions(StackGresShardedDbOps context) {
    return Optional.ofNullable(context.getStatus())
        .map(StackGresShardedDbOpsStatus::getConditions)
        .orElse(List.of());
  }

  @Override
  protected void setConditions(StackGresShardedDbOps context, List<Condition> conditions) {
    if (context.getStatus() == null) {
      context.setStatus(new StackGresShardedDbOpsStatus());
    }
    context.getStatus().setConditions(conditions);
  }

}
