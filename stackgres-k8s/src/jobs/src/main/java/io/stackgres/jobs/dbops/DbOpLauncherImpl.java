/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import static io.stackgres.jobs.app.JobsProperty.DBOPS_LOCK_POLL_INTERVAL;
import static io.stackgres.jobs.app.JobsProperty.DBOPS_LOCK_TIMEOUT;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
public class DbOpLauncherImpl implements DbOpLauncher {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbOpLauncherImpl.class);

  @Inject
  CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  @Inject
  CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;

  @Inject
  LockAcquirer<StackGresCluster> lockAcquirer;

  @Inject
  @Any
  Instance<DatabaseOperationJob> instance;

  @Inject
  DatabaseOperationEventEmitter databaseOperationEventEmitter;

  private final ExecutorService jobExecutor;

  public DbOpLauncherImpl() {
    this.jobExecutor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder()
        .setNameFormat("job-executor-%d")
        .build());
  }

  @Override
  public void launchDbOp(String dbOpName, String namespace) {
    StackGresDbOps dbOps = dbOpsFinder.findByNameAndNamespace(dbOpName, namespace)
        .orElseThrow(() -> new IllegalArgumentException(StackGresDbOps.KIND + " "
            + dbOpName + " does not exists in namespace " + namespace));

    Instance<DatabaseOperationJob> jobImpl =
        instance.select(new DatabaseOperationLiteral(dbOps.getSpec().getOp()));

    if (jobImpl.isResolvable()) {
      LOGGER.info("Initializing conditions for SgDbOps {}", dbOps.getMetadata().getName());
      var status = Optional.ofNullable(dbOps.getStatus())
          .or(() -> Optional.of(new StackGresDbOpsStatus()))
          .map(dbOpsStatus -> {
            dbOpsStatus.setOpStarted(Instant.now().toString());
            dbOpsStatus.setOpRetries(
                Optional.ofNullable(dbOpsStatus.getOpRetries())
                .map(opRetries -> opRetries + 1)
                .orElse(0));
            dbOpsStatus.setConditions(getStartingConditions());
            return dbOpsStatus;
          })
          .orElseThrow();
      dbOps.setStatus(status);
      final StackGresDbOps initializedDbOps = dbOpsScheduler.update(dbOps);

      try {
        final int lockPollInterval = Integer.parseInt(DBOPS_LOCK_POLL_INTERVAL.getString());
        final int timeout = Integer.parseInt(DBOPS_LOCK_TIMEOUT.getString());

        LockRequest lockRequest = ImmutableLockRequest.builder()
            .namespace(initializedDbOps.getMetadata().getNamespace())
            .serviceAccount(JobsProperty.SERVICE_ACCOUNT.getString())
            .podName(JobsProperty.POD_NAME.getString())
            .pollInterval(lockPollInterval)
            .timeout(timeout)
            .lockResourceName(initializedDbOps.getSpec().getSgCluster())
            .build();

        Infrastructure.setDroppedExceptionHandler(err -> LOGGER.error("Dropped exception ", err));

        lockAcquirer
            .lockRun(lockRequest, (targetCluster) -> {
              databaseOperationEventEmitter.operationStarted(dbOpName, namespace);
              var jobUni = Uni.createFrom().voidItem()
                  .emitOn(jobExecutor)
                  .chain(() -> jobImpl.get()
                      .runJob(initializedDbOps, targetCluster));
              Optional.ofNullable(initializedDbOps.getSpec().getTimeout())
                  .map(Duration::parse)
                  .map(jobTimeout -> jobUni.await().atMost(jobTimeout))
                  .orElseGet(() -> jobUni.await().indefinitely());
              databaseOperationEventEmitter.operationCompleted(dbOpName, namespace);
            });

        LOGGER.info("Operation completed for SgDbOp {}", dbOpName);
        updateToCompletedConditions(dbOpName, namespace);
      } catch (TimeoutException timeoutEx) {
        updateToTimeoutConditions(dbOpName, namespace);
        databaseOperationEventEmitter.operationTimedOut(dbOpName, namespace);
        throw timeoutEx;
      } catch (Exception e) {
        LOGGER.info("Unexpected exception for SgDbOp {}", dbOpName, e);
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
        DbOpsStatusCondition.DB_OPS_RUNNING.getCondition(),
        DbOpsStatusCondition.DB_OPS_FALSE_COMPLETED.getCondition(),
        DbOpsStatusCondition.DB_OPS_FALSE_FAILED.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    return conditions;
  }

  public List<Condition> getCompletedConditions() {
    final List<Condition> conditions = List.of(
        DbOpsStatusCondition.DB_OPS_FALSE_RUNNING.getCondition(),
        DbOpsStatusCondition.DB_OPS_COMPLETED.getCondition(),
        DbOpsStatusCondition.DB_OPS_FALSE_FAILED.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    return conditions;
  }

  public List<Condition> getFailedConditions() {
    final List<Condition> conditions = List.of(
        DbOpsStatusCondition.DB_OPS_FALSE_RUNNING.getCondition(),
        DbOpsStatusCondition.DB_OPS_FALSE_COMPLETED.getCondition(),
        DbOpsStatusCondition.DB_OPS_FAILED.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    return conditions;
  }

  public List<Condition> getTimeoutConditions() {
    final List<Condition> conditions = List.of(
        DbOpsStatusCondition.DB_OPS_FALSE_RUNNING.getCondition(),
        DbOpsStatusCondition.DB_OPS_FALSE_COMPLETED.getCondition(),
        DbOpsStatusCondition.DB_OPS_TIMED_OUT.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    return conditions;
  }

}
