/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
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
    if (isLocked(targetCluster, target) && !isLockedByMe(targetCluster, target)) {
      LOGGER.info("Locked cluster {}, waiting for release", clusterId);
      while (isLocked(targetCluster, target)) {
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
          .invoke(cluster -> cluster.getMetadata().getAnnotations().remove(LOCK_POD))
          .invoke(cluster -> cluster.getMetadata().getAnnotations().remove(LOCK_TIMESTAMP))
          .invoke(clusterScheduler::update)
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

  private String getLockTimestamp() {
    return Long.toString(System.currentTimeMillis() / 1000);
  }

  private StackGresCluster lock(LockRequest target, StackGresCluster targetCluster) {
    final Map<String, String> annotations = targetCluster.getMetadata().getAnnotations();

    annotations.put(LOCK_POD, target.getPodName());
    annotations.put(LOCK_TIMESTAMP, getLockTimestamp());
    return clusterScheduler.update(targetCluster);

  }

  private void lock(LockRequest target) {
    var targetCluster = getCluster(target);
    lock(target, targetCluster);
  }

  private boolean isLocked(StackGresCluster cluster, LockRequest lockRequest) {
    long currentTimeSeconds = System.currentTimeMillis() / 1000;
    long timedOutLock = currentTimeSeconds - lockRequest.getLockTimeout();
    return Optional.ofNullable(cluster.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .filter(annotation ->
            annotation.containsKey(LOCK_POD) && annotation.containsKey(LOCK_TIMESTAMP))
        .map(annotations -> Long.parseLong(annotations.get(LOCK_TIMESTAMP)))
        .map(lockTimestamp -> lockTimestamp > timedOutLock)
        .orElse(false);
  }

  private boolean isLockedByMe(StackGresCluster cluster, LockRequest lockRequest) {
    return Optional.ofNullable(cluster.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .filter(annotation ->
            annotation.containsKey(LOCK_POD) && annotation.containsKey(LOCK_TIMESTAMP))
        .map(annotation -> annotation.get(LOCK_POD).equals(lockRequest.getPodName()))
        .orElse(false);
  }
}
