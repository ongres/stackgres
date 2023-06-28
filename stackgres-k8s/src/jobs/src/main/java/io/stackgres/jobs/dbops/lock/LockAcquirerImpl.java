/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import java.time.Duration;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.jobs.dbops.MutinyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LockAcquirerImpl implements LockAcquirer<StackGresCluster> {

  private static final Logger LOGGER = LoggerFactory.getLogger(LockAcquirerImpl.class);
  private static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(
      1,
      new ThreadFactoryBuilder()
          .setNameFormat("LockAcquirerThread-%d")
          .build());
  @Inject
  CustomResourceScheduler<StackGresCluster> clusterScheduler;

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Override
  public void lockRun(LockRequest target, Consumer<StackGresCluster> task) {
    StackGresCluster targetCluster = getCluster(target);
    String clusterId = targetCluster.getMetadata().getNamespace()
        + "/" + targetCluster.getMetadata().getName();
    LOGGER.info("Acquiring lock for cluster {}", clusterId);
    if (StackGresUtil.isLocked(targetCluster, target.getTimeout())
        && !StackGresUtil.isLockedByMe(targetCluster, target.getPodName())) {
      LOGGER.info("Locked cluster {}, waiting for release", clusterId);
      while (StackGresUtil.isLocked(targetCluster, target.getTimeout())) {
        try {
          Thread.sleep(target.getPollInterval() * 1000L);
          targetCluster = getCluster(target);
        } catch (InterruptedException e) {
          LOGGER.error("Interrupted while waiting for lock", e);
        }
      }
    }

    targetCluster = lock(target, targetCluster);
    LOGGER.info("Cluster {} locked", clusterId);

    var lockFuture = EXECUTOR.scheduleAtFixedRate(
        () -> lock(target),
        target.getPollInterval(),
        target.getPollInterval(),
        TimeUnit.SECONDS);

    try {
      LOGGER.info("Executing locked task");
      task.accept(targetCluster);
    } catch (Exception e) {
      LOGGER.error("Locked task failed", e);
      throw e;
    } finally {
      lockFuture.cancel(true);
      Uni.createFrom().item(() -> getCluster(target))
          .invoke(StackGresUtil::resetLock)
          .invoke(clusterScheduler::update)
          .onFailure()
          .transform(MutinyUtil.logOnFailureToRetry("updating the lock"))
          .onFailure()
          .retry()
          .withBackOff(Duration.ofMillis(5), Duration.ofSeconds(5))
          .indefinitely()
          .await().indefinitely();
    }
  }

  private StackGresCluster getCluster(LockRequest target) {
    return clusterFinder
        .findByNameAndNamespace(target.getLockResourceName(), target.getNamespace())
        .orElseThrow();
  }

  private long getLockTimestamp() {
    return System.currentTimeMillis() / 1000;
  }

  private StackGresCluster lock(LockRequest target) {
    var targetCluster = getCluster(target);
    return lock(target, targetCluster);
  }

  private StackGresCluster lock(LockRequest target, StackGresCluster targetCluster) {
    StackGresUtil.setLock(targetCluster, target.getServiceAccount(), target.getPodName(),
        getLockTimestamp());
    clusterScheduler.update(targetCluster);
    return targetCluster;
  }

}
