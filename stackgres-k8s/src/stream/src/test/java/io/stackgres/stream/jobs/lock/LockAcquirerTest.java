/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.lock;

import static io.stackgres.common.StackGresContext.LOCK_POD_KEY;
import static io.stackgres.common.StackGresContext.LOCK_TIMEOUT_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LockAcquirerTest {

  private final AtomicInteger streamNr = new AtomicInteger(0);
  @Inject
  LockAcquirer lockAcquirer;
  @Inject
  MockKubeDb kubeDb;
  private StackGresStream stream;
  private String streamName;
  private String streamNamespace;
  private LockRequest lockRequest;
  private ExecutorService executorService;

  private static LockRequest buildLockRequest(StackGresStream stream) {
    return LockRequest.builder()
        .serviceAccount(StringUtils.getRandomString())
        .podName(StringUtils.getRandomString())
        .namespace(stream.getMetadata().getNamespace())
        .lockResourceName(stream.getMetadata().getName())
        .duration(30)
        .pollInterval(1)
        .build();
  }

  @BeforeEach
  void setUp() {
    stream = Fixtures.stream().loadSgClusterToCloudEvent().get();
    stream.getMetadata().setName("test-" + streamNr.incrementAndGet());
    streamName = stream.getMetadata().getName();
    streamNamespace = stream.getMetadata().getNamespace();
    lockRequest = buildLockRequest(stream);
    executorService = Executors.newSingleThreadExecutor();

  }

  @AfterEach
  void tearDown() {
    executorService.shutdownNow();
    kubeDb.delete(stream);
  }

  @Test
  void givenAnUnlockedStream_itShouldAcquireTheLockBeforeRunningTheTask() {
    prepareUnlockedCLuster();

    AtomicBoolean taskRunned = new AtomicBoolean(false);
    lockAcquirer.lockRun(lockRequest, Uni.createFrom().voidItem().invoke(item -> {
      final StackGresStream storedStream = kubeDb
          .getStream(streamName, streamNamespace);
      final Map<String, String> annotations = storedStream
          .getMetadata().getAnnotations();
      assertNotNull(annotations.get(LOCK_POD_KEY));
      assertEquals(lockRequest.getPodName(), annotations.get(LOCK_POD_KEY));
      assertNotNull(annotations.get(LOCK_TIMEOUT_KEY));
      taskRunned.set(true);
    })).await().indefinitely();

    assertTrue(taskRunned.get());
  }

  @Test
  void givenAnUnlockedStream_itShouldReleaseTheLockIfTheTaskExitsSuccessfully() {
    prepareUnlockedCLuster();

    runTaskSuccessfully();

    StackGresStream lastPatch = kubeDb.getStream(streamName, streamNamespace);
    assertNull(lastPatch.getMetadata().getAnnotations().get(LOCK_POD_KEY));
    assertNull(lastPatch.getMetadata().getAnnotations().get(LOCK_TIMEOUT_KEY));
  }

  @Test
  void givenALockedStreamByMe_itShouldUpdateTheLockTimestampBeforeRunningTheTask() {
    final long lockTimeout = (System.currentTimeMillis() / 1000) - 1;
    prepareLockedStream(lockRequest.getPodName(), lockTimeout);

    AtomicBoolean taskRunned = new AtomicBoolean(false);

    lockAcquirer.lockRun(lockRequest, Uni.createFrom().voidItem().invoke(item -> {
      taskRunned.set(true);
      StackGresStream lastPatch = kubeDb.getStream(streamName, streamNamespace);
      final Map<String, String> annotations = lastPatch.getMetadata().getAnnotations();
      assertNotNull(annotations.get(LOCK_POD_KEY));
      assertNotNull(annotations.get(LOCK_TIMEOUT_KEY));
      assertTrue(Long.parseLong(annotations.get(LOCK_TIMEOUT_KEY)) > lockTimeout);
    })).await().indefinitely();
  }

  @Test
  void givenALockedStream_itShouldWaitUntilTheLockIsReleasedBeforeRunningTheTask() {
    final long lockTimeout =
        (System.currentTimeMillis() / 1000) + lockRequest.getPollInterval() + 1;
    prepareLockedStream(StringUtils.getRandomString(), lockTimeout);

    AtomicBoolean taskRan = asycRunTaskSuccessfully();

    sleep(lockRequest.getPollInterval() + 1);

    assertFalse(taskRan.get());

    removeLock();

    sleep(lockRequest.getPollInterval() + 2);

    assertTrue(taskRan.get());
  }

  @Test
  void givenATimedoutLockedStream_itShouldOverrideTheLock() {
    final long lockTimeout =
        (System.currentTimeMillis() / 1000) - lockRequest.getDuration() - 1;
    prepareLockedStream(lockRequest.getLockResourceName(), lockTimeout);

    AtomicBoolean taskRan = asycRunTaskSuccessfully();

    assertFalse(taskRan.get());

    sleep(lockRequest.getPollInterval() + 1);

    assertTrue(taskRan.get());
  }

  @Test
  void givenALongRunningTask_itShouldUpdateTheLockTimestampPeriodically() {
    prepareUnlockedCLuster();

    AtomicBoolean taskRan = asycRunTaskSuccessfully(3);

    assertFalse(taskRan.get());

    sleep(lockRequest.getPollInterval() + 1);

    long lockTimeout = Long.parseLong(kubeDb.getStream(streamName, streamNamespace)
        .getMetadata().getAnnotations().get(LOCK_TIMEOUT_KEY));
    long currentTimestamp = System.currentTimeMillis() / 1000;
    long elapsedAfterLock = currentTimestamp - lockTimeout - lockRequest.getDuration();
    assertTrue(elapsedAfterLock <= lockRequest.getPollInterval());

    sleep(lockRequest.getPollInterval() + 3);

    assertTrue(taskRan.get());
  }

  private void removeLock() {
    var stream = kubeDb.getStream(streamName, streamNamespace);
    stream.getMetadata().getAnnotations().remove(LOCK_POD_KEY);
    stream.getMetadata().getAnnotations().remove(LOCK_TIMEOUT_KEY);
    kubeDb.addOrReplaceStream(stream);
  }

  private void runTaskSuccessfully() {
    AtomicBoolean taskRan = new AtomicBoolean(false);

    lockAcquirer.lockRun(lockRequest, Uni.createFrom().voidItem().invoke(item -> {
      StackGresStream lastPatch = kubeDb.getStream(streamName, streamNamespace);
      final Map<String, String> annotations = lastPatch.getMetadata().getAnnotations();
      assertEquals(lockRequest.getPodName(), annotations.get(LOCK_POD_KEY));
      taskRan.set(true);
    })).await().indefinitely();

    assertTrue(taskRan.get());
  }

  private AtomicBoolean asycRunTaskSuccessfully() {
    return asycRunTaskSuccessfully(0);
  }

  private AtomicBoolean asycRunTaskSuccessfully(int delay) {
    AtomicBoolean taskRan = new AtomicBoolean(false);

    executorService.execute(
        () -> lockAcquirer.lockRun(lockRequest,
            Uni.createFrom().voidItem().invoke(item -> {
              if (delay > 0) {
                sleep(delay);
              }
              StackGresStream lastPatch = kubeDb.getStream(streamName, streamNamespace);
              final Map<String, String> annotations = lastPatch.getMetadata().getAnnotations();
              assertEquals(lockRequest.getPodName(), annotations.get(LOCK_POD_KEY),
                  "Task ran without Lock!!");
              assertNotNull(annotations.get(LOCK_TIMEOUT_KEY));
              taskRan.set(true);
            })).await().indefinitely());

    return taskRan;
  }

  private void prepareUnlockedCLuster() {
    StackGresStream stream = kubeDb.getStream(streamName, streamNamespace);
    if (stream == null) {
      stream = this.stream;
    }
    stream.setStatus(null);
    final Map<String, String> annotations = stream.getMetadata().getAnnotations();
    annotations.remove(LOCK_POD_KEY);
    annotations.remove(LOCK_TIMEOUT_KEY);
    kubeDb.addOrReplaceStream(stream);
  }

  private void prepareLockedStream(String lockPod, Long lockTimeout) {
    StackGresStream stream = kubeDb.getStream(streamName, streamNamespace);
    if (stream == null) {
      stream = this.stream;
    }
    stream.setStatus(null);
    final Map<String, String> annotations = stream.getMetadata().getAnnotations();
    annotations.put(LOCK_POD_KEY, lockPod);
    annotations.put(LOCK_TIMEOUT_KEY, Long.toString(lockTimeout));
    kubeDb.addOrReplaceStream(stream);
  }

  private void sleep(int seconds) {
    try {
      Thread.sleep(seconds * 1000L);
    } catch (InterruptedException ignored) {
      // ignored
    }
  }

}
