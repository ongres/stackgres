/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import static io.stackgres.jobs.app.JobsProperty.DBOPS_LOCK_DURATION;
import static io.stackgres.jobs.app.JobsProperty.DBOPS_LOCK_POLL_INTERVAL;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.jobs.app.JobsProperty;
import io.stackgres.jobs.dbops.lock.ImmutableLockRequest;
import io.stackgres.jobs.dbops.lock.LockAcquirer;
import io.stackgres.jobs.dbops.lock.LockRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DbOpsLauncher {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbOpsLauncher.class);

  @Inject
  CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;

  @Inject
  LockAcquirer lockAcquirer;

  @Inject
  @Any
  Instance<DatabaseOperationJob> instance;

  @Inject
  DatabaseOperationEventEmitter databaseOperationEventEmitter;

  public void launchDbOp(String dbOpName, String namespace) {
    StackGresDbOps dbOps = dbOpsFinder.findByNameAndNamespace(dbOpName, namespace)
        .orElseThrow(() -> new IllegalArgumentException(StackGresDbOps.KIND + " "
            + dbOpName + " does not exists in namespace " + namespace));

    Instance<DatabaseOperationJob> jobImpl =
        instance.select(new DatabaseOperationLiteral(dbOps.getSpec().getOp()));

    if (jobImpl.isResolvable()) {
      LOGGER.info("Initializing conditions for SGDbOps {}", dbOps.getMetadata().getName());
      final StackGresDbOps initializedDbOps = dbOpsScheduler.update(dbOps,
          (currentDbOps) -> {
            var status = Optional.ofNullable(currentDbOps.getStatus())
                .or(() -> Optional.of(new StackGresDbOpsStatus()))
                .map(dbOpsStatus -> {
                  dbOpsStatus.setOpStarted(Instant.now().toString());
                  dbOpsStatus.setConditions(getStartingConditions());
                  return dbOpsStatus;
                })
                .orElseThrow();
            currentDbOps.setStatus(status);
          });

      try {
        final int lockPollInterval = Integer.parseInt(DBOPS_LOCK_POLL_INTERVAL.getString());
        final int duration = Integer.parseInt(DBOPS_LOCK_DURATION.getString());

        LockRequest lockRequest = ImmutableLockRequest.builder()
            .namespace(initializedDbOps.getMetadata().getNamespace())
            .serviceAccount(JobsProperty.SERVICE_ACCOUNT.getString())
            .podName(JobsProperty.POD_NAME.getString())
            .pollInterval(lockPollInterval)
            .duration(duration)
            .lockResourceName(initializedDbOps.getSpec().getSgCluster())
            .build();

        Infrastructure.setDroppedExceptionHandler(err -> LOGGER.error("Dropped exception ", err));

        StackGresCluster cluster = clusterFinder.findByNameAndNamespace(
            dbOps.getSpec().getSgCluster(),
            namespace)
            .orElseThrow(() -> new IllegalArgumentException(StackGresCluster.KIND + " "
                + dbOps.getSpec().getSgCluster() + " does not exists in namespace " + namespace));
        var dbOpsUni =
            lockAcquirer.lockRun(lockRequest, Uni.createFrom().voidItem()
                .invoke(() -> databaseOperationEventEmitter
                    .operationStarted(dbOpName, namespace))
                .chain(() -> jobImpl.get()
                    .runJob(initializedDbOps, cluster))
                .invoke(() -> databaseOperationEventEmitter
                    .operationCompleted(dbOpName, namespace)))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        Optional.ofNullable(initializedDbOps.getSpec().getTimeout())
            .map(Duration::parse)
            .ifPresentOrElse(
                jobTimeout -> dbOpsUni.await().atMost(jobTimeout),
                () -> dbOpsUni.await().indefinitely());

        LOGGER.info("Operation completed for SGDbOps {}", dbOpName);
        updateToCompletedConditions(dbOpName, namespace);
      } catch (TimeoutException timeoutEx) {
        updateToTimeoutConditions(dbOpName, namespace);
        databaseOperationEventEmitter.operationTimedOut(dbOpName, namespace);
        throw timeoutEx;
      } catch (Exception e) {
        LOGGER.info("Unexpected exception for SGDbOps {}", dbOpName, e);
        updateToFailedConditions(dbOpName, namespace);
        databaseOperationEventEmitter.operationFailed(dbOpName, namespace);
        throw e;
      }
    } else if (jobImpl.isAmbiguous()) {
      throw new IllegalStateException("Multiple implementations of the operation "
          + dbOps.getSpec().getOp() + " found");
    } else {
      throw new IllegalStateException("Implementation of operation "
          + dbOps.getSpec().getOp()
          + " not found");
    }
  }

  private void updateToConditions(String dbOpName, String namespace, List<Condition> conditions) {
    Uni.createFrom().item(() -> dbOpsFinder.findByNameAndNamespace(dbOpName, namespace)
            .orElseThrow())
        .invoke(currentDbOps -> currentDbOps.getStatus().setConditions(conditions))
        .invoke(dbOpsScheduler::update)
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("updating conditions for SGDbOps"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
        .indefinitely()
        .await().indefinitely();
  }

  private void updateToCompletedConditions(String dbOpName, String namespace) {
    updateToConditions(dbOpName, namespace, getCompletedConditions());
  }

  private void updateToFailedConditions(String dbOpName, String namespace) {
    updateToConditions(dbOpName, namespace, getFailedConditions());
  }

  private void updateToTimeoutConditions(String dbOpName, String namespace) {
    updateToConditions(dbOpName, namespace, getTimeoutConditions());
  }

  public List<Condition> getStartingConditions() {
    final List<Condition> conditions = List.of(
        DbOpsStatusCondition.DBOPS_RUNNING.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_FAILED.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    return conditions;
  }

  public List<Condition> getCompletedConditions() {
    final List<Condition> conditions = List.of(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        DbOpsStatusCondition.DBOPS_COMPLETED.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_FAILED.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    return conditions;
  }

  public List<Condition> getFailedConditions() {
    final List<Condition> conditions = List.of(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        DbOpsStatusCondition.DBOPS_FAILED.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    return conditions;
  }

  public List<Condition> getTimeoutConditions() {
    final List<Condition> conditions = List.of(
        DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition(),
        DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition(),
        DbOpsStatusCondition.DBOPS_TIMED_OUT.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    return conditions;
  }

}
