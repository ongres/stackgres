/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.lock;

import java.time.Duration;

import io.smallrye.mutiny.Uni;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.stream.jobs.MutinyUtil;
import io.stackgres.stream.jobs.StreamExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LockAcquirer {

  private static final Logger LOGGER = LoggerFactory.getLogger(LockAcquirer.class);

  @Inject
  CustomResourceScheduler<StackGresStream> streamScheduler;

  @Inject
  CustomResourceFinder<StackGresStream> streamFinder;

  @Inject
  StreamExecutorService executorService;

  public Uni<?> lockRun(LockRequest lockRequest, Uni<?> task) {
    return executorService.itemAsync(() -> getStream(lockRequest))
        .invoke(stream -> LOGGER.info("Acquiring lock for stream {}",
            stream.getMetadata().getName()))
        .invoke(stream -> acquireLock(lockRequest, stream))
        .onFailure(RetryLockException.class)
        .retry()
        .withBackOff(
            Duration.ofSeconds(lockRequest.getPollInterval()),
            Duration.ofSeconds(lockRequest.getPollInterval()))
        .indefinitely()
        .invoke(stream -> LOGGER.info("Stream {} lock acquired",
            stream.getMetadata().getName()))
        .invoke(() -> LOGGER.info("Executing locked task"))
        .chain(stream -> Uni.combine().any().of(
            task
                .onFailure()
                .invoke(ex -> LOGGER.error("Locked task failed", ex))
                .chain(() -> Uni.createFrom().voidItem()),
            Uni.createFrom().voidItem()
                .chain(() -> executorService.invokeAsync(() -> refreshLock(lockRequest, stream)))
                .onItem()
                .delayIt()
                .by(Duration.ofSeconds(lockRequest.getPollInterval()))
                .repeat()
                .indefinitely()
                .skip()
                .where(ignored -> true)
                .toUni()
                .onFailure()
                .transform(MutinyUtil.logOnFailureToRetry("updating the lock")))
            .onItemOrFailure()
            .call((result, ex) -> Uni.createFrom().voidItem()
                .chain(() -> executorService.invokeAsync(() -> releaseLock(lockRequest, stream)))
                .invoke(() -> LOGGER.info("Stream {} lock released",
                    stream.getMetadata().getName()))
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

  private StackGresStream getStream(LockRequest lockRequest) {
    return streamFinder
        .findByNameAndNamespace(lockRequest.getLockResourceName(), lockRequest.getNamespace())
        .orElseThrow();
  }

  private void acquireLock(LockRequest lockRequest, StackGresStream stream) {
    streamScheduler.update(stream, foundStream -> {
      if (StackGresUtil.isLocked(foundStream)
          && !StackGresUtil.isLockedBy(foundStream, lockRequest.getPodName())) {
        LOGGER.info("Stream {} is locked, waiting for release",
            stream.getMetadata().getName());
        throw new RetryLockException();
      }
      StackGresUtil.setLock(
          foundStream, lockRequest.getServiceAccount(),
          lockRequest.getPodName(), lockRequest.getDuration());
    });
  }

  private void refreshLock(LockRequest lockRequest, StackGresStream stream) {
    streamScheduler.update(stream, foundStream -> {
      if (!StackGresUtil.isLockedBy(foundStream, lockRequest.getPodName())) {
        LOGGER.error("Lock lost for stream {}", stream.getMetadata().getName());
        throw new RuntimeException(
            "Lock lost for stream " + stream.getMetadata().getName());
      }
      StackGresUtil.setLock(
          foundStream, lockRequest.getServiceAccount(),
          lockRequest.getPodName(), lockRequest.getDuration());
    });
  }

  private void releaseLock(LockRequest lockRequest, StackGresStream stream) {
    streamScheduler.update(stream, foundStream -> {
      if (!StackGresUtil.isLockedBy(foundStream, lockRequest.getPodName())) {
        return;
      }
      StackGresUtil.resetLock(foundStream);
    });
  }

}
