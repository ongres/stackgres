/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.stackgres.common.DbOpsUtil;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DbOpsStatusManager
    extends ConditionUpdater<StackGresDbOps, Condition>
    implements StatusManager<StackGresDbOps, Condition> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbOpsStatusManager.class);

  private final ResourceFinder<Job> jobFinder;

  @Inject
  public DbOpsStatusManager(ResourceFinder<Job> jobFinder) {
    this.jobFinder = jobFinder;
  }

  private static String getDbOpsId(StackGresDbOps dbOps) {
    return dbOps.getMetadata().getNamespace() + "/" + dbOps.getMetadata().getName();
  }

  @Override
  public StackGresDbOps refreshCondition(StackGresDbOps source) {
    final boolean isJobFailedAndStatusNotUpdated;
    if (Optional.of(source)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .filter(condition -> Objects.equals(condition.getType(),
            DbOpsStatusCondition.Type.COMPLETED.getType())
            || Objects.equals(condition.getType(),
                DbOpsStatusCondition.Type.FAILED.getType()))
        .anyMatch(condition -> Objects.equals(condition.getStatus(), "True"))) {
      isJobFailedAndStatusNotUpdated = false;
    } else {
      final Optional<Job> job = jobFinder.findByNameAndNamespace(
          DbOpsUtil.jobName(source),
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
        source.setStatus(new StackGresDbOpsStatus());
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
      LOGGER.debug("DbOps {} failed since the job failed but status condition"
          + " is neither completed or failed", getDbOpsId(source));
      updateCondition(getFalseRunning(), source);
      updateCondition(getFalseCompleted(), source);
      updateCondition(getFailedDueToUnexpectedFailure(), source);
    }
    return source;
  }

  protected Condition getFalseRunning() {
    return DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition();
  }

  protected Condition getFalseCompleted() {
    return DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition();
  }

  protected Condition getFailedDueToUnexpectedFailure() {
    var failed = DbOpsStatusCondition.DBOPS_FAILED.getCondition();
    failed.setMessage("Unexpected failure");
    return failed;
  }

  @Override
  protected List<Condition> getConditions(StackGresDbOps context) {
    return Optional.ofNullable(context.getStatus())
        .map(StackGresDbOpsStatus::getConditions)
        .orElse(List.of());
  }

  @Override
  protected void setConditions(StackGresDbOps context, List<Condition> conditions) {
    if (context.getStatus() == null) {
      context.setStatus(new StackGresDbOpsStatus());
    }
    context.getStatus().setConditions(conditions);
  }

}
