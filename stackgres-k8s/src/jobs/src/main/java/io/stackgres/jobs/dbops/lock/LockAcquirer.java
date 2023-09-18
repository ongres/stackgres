/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.jobs.dbops.MutinyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LockAcquirer {

  private static final Logger LOGGER = LoggerFactory.getLogger(LockAcquirer.class);

  @Inject
  CustomResourceScheduler<StackGresCluster> clusterScheduler;

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  public Uni<?> lockRun(LockRequest lockRequest, Uni<?> task) {
    return Uni.createFrom().item(() -> getCluster(lockRequest))
        .invoke(cluster -> LOGGER.info("Acquiring lock for cluster {}",
            cluster.getMetadata().getName()))
        .invoke(cluster -> acquireLock(lockRequest, cluster))
        .onFailure(RetryLockException.class)
        .retry()
        .withBackOff(
            Duration.ofSeconds(lockRequest.getPollInterval()),
            Duration.ofSeconds(lockRequest.getPollInterval()))
        .indefinitely()
        .invoke(cluster -> LOGGER.info("Cluster {} lock acquired",
            cluster.getMetadata().getName()))
        .invoke(() -> LOGGER.info("Executing locked task"))
        .chain(cluster -> Uni.combine().any().of(
            task
                .onFailure()
                .invoke(ex -> LOGGER.error("Locked task failed", ex))
                .chain(() -> Uni.createFrom().voidItem())
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()),
            Uni.createFrom().voidItem()
                .invoke(() -> refreshLock(lockRequest, cluster))
                .onItem()
                .delayIt()
                .by(Duration.ofSeconds(lockRequest.getPollInterval()))
                .repeat()
                .indefinitely()
                .skip()
                .where(ignored -> true)
                .toUni()
                .onFailure()
                .transform(MutinyUtil.logOnFailureToRetry("updating the lock"))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()))
            .onItemOrFailure()
            .call((result, ex) -> Uni.createFrom().voidItem()
                .invoke(() -> releaseLock(lockRequest, cluster))
                .invoke(() -> LOGGER.info("Cluster {} lock released",
                    cluster.getMetadata().getName()))
                .onFailure()
                .transform(MutinyUtil.logOnFailureToRetry("releasing the lock"))
                .onFailure()
                .retry()
                .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
                .atMost(10)
                .invoke(() -> {
                  if (ex != null) {
                    throw new RetryLockException(ex);
                  }
                })
                .onFailure(RetryLockException.class)
                .transform(Throwable::getCause)));
  }

  private StackGresCluster getCluster(LockRequest lockRequest) {
    return clusterFinder
        .findByNameAndNamespace(lockRequest.getLockResourceName(), lockRequest.getNamespace())
        .orElseThrow();
  }

  private void acquireLock(LockRequest lockRequest, StackGresCluster cluster) {
    clusterScheduler.update(cluster, foundCluster -> {
      if (StackGresUtil.isLocked(foundCluster)
          && !StackGresUtil.isLockedBy(foundCluster, lockRequest.getPodName())) {
        LOGGER.info("Cluster {} is locked, waiting for release",
            cluster.getMetadata().getName());
        throw new RetryLockException();
      }
      StackGresUtil.setLock(
          foundCluster, lockRequest.getServiceAccount(),
          lockRequest.getPodName(), lockRequest.getDuration());
    });
  }

  private void refreshLock(LockRequest lockRequest, StackGresCluster cluster) {
    clusterScheduler.update(cluster, foundCluster -> {
      if (!StackGresUtil.isLockedBy(foundCluster, lockRequest.getPodName())) {
        LOGGER.error("Lock lost for cluster {}", cluster.getMetadata().getName());
        throw new RuntimeException(
            "Lock lost for cluster " + cluster.getMetadata().getName());
      }
      StackGresUtil.setLock(
          foundCluster, lockRequest.getServiceAccount(),
          lockRequest.getPodName(), lockRequest.getDuration());
    });
  }

  private void releaseLock(LockRequest lockRequest, StackGresCluster cluster) {
    clusterScheduler.update(cluster, foundCluster -> {
      if (!StackGresUtil.isLockedBy(foundCluster, lockRequest.getPodName())) {
        return;
      }
      StackGresUtil.resetLock(foundCluster);
    });
  }

}
