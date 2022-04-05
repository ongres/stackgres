/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.DbOpsUtil;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DbOpsStatusManager
    extends ConditionUpdater<StackGresDbOps, StackGresDbOpsCondition>
    implements StatusManager<StackGresDbOps, StackGresDbOpsCondition> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbOpsStatusManager.class);

  private final KubernetesClient client;

  @Inject
  public DbOpsStatusManager(KubernetesClient client) {
    this.client = client;
  }

  private static String getDbOpsId(StackGresDbOps dbOps) {
    return dbOps.getMetadata().getNamespace() + "/" + dbOps.getMetadata().getName();
  }

  @Override
  public StackGresDbOps refreshCondition(StackGresDbOps source) {
    if (isJobFailedAndStatusNotUpdated(source)) {
      LOGGER.debug("DbOps {} failed since the job failed but status condition"
          + " is neither completed or failed", getDbOpsId(source));
      updateCondition(getFalseRunning(), source);
      updateCondition(getFalseCompleted(), source);
      updateCondition(getFailed(), source);
    }
    return source;
  }

  public boolean isJobFailedAndStatusNotUpdated(StackGresDbOps source) {
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
      return false;
    }
    Optional<Job> job = Optional.ofNullable(client.batch().v1().jobs()
        .inNamespace(source.getMetadata().getNamespace())
        .withName(DbOpsUtil.jobName(source))
        .get());
    return job
        .map(Job::getStatus)
        .map(JobStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .anyMatch(condition -> Objects.equals(condition.getType(), "Failed")
            && Objects.equals(condition.getStatus(), "True"));
  }

  protected StackGresDbOpsCondition getFalseRunning() {
    return DbOpsStatusCondition.DB_OPS_FALSE_RUNNING.getCondition();
  }

  protected StackGresDbOpsCondition getFalseCompleted() {
    return DbOpsStatusCondition.DB_OPS_FALSE_COMPLETED.getCondition();
  }

  protected StackGresDbOpsCondition getFailed() {
    return DbOpsStatusCondition.DB_OPS_FAILED.getCondition();
  }

  @Override
  protected List<StackGresDbOpsCondition> getConditions(StackGresDbOps context) {
    return Optional.ofNullable(context.getStatus())
        .map(StackGresDbOpsStatus::getConditions)
        .orElseGet(ArrayList::new);
  }

  @Override
  protected void setConditions(StackGresDbOps context, List<StackGresDbOpsCondition> conditions) {
    if (context.getStatus() == null) {
      context.setStatus(new StackGresDbOpsStatus());
    }
    context.getStatus().setConditions(conditions);
  }

}
