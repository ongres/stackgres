/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import static io.stackgres.common.StackGresContext.LOCK_POD_KEY;
import static io.stackgres.common.StackGresContext.LOCK_TIMESTAMP_KEY;
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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LockAcquirerImplTest {

  private final AtomicInteger clusterNr = new AtomicInteger(0);
  @Inject
  LockAcquirerImpl lockAcquirer;
  @Inject
  MockKubeDb kubeDb;
  private StackGresCluster cluster;
  private String clusterName;
  private String clusterNamespace;
  private LockRequest lockRequest;
  private ExecutorService executorService;

  private static LockRequest buildLockRequest(StackGresCluster cluster) {

    return ImmutableLockRequest.builder()
        .serviceAccount(StringUtils.getRandomString())
        .podName(StringUtils.getRandomString())
        .namespace(cluster.getMetadata().getNamespace())
        .lockResourceName(cluster.getMetadata().getName())
        .timeout(30)
        .pollInterval(1)
        .build();

  }

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().setName("test-" + clusterNr.incrementAndGet());
    clusterName = cluster.getMetadata().getName();
    clusterNamespace = cluster.getMetadata().getNamespace();
    lockRequest = buildLockRequest(cluster);
    executorService = Executors.newSingleThreadExecutor();

  }

  @AfterEach
  void tearDown() {
    executorService.shutdownNow();
    kubeDb.delete(cluster);
  }

  @Test
  void givenAnUnlockedCluster_itShouldAcquireTheLockBeforeRunningTheTask() {

    prepareUnlockedCLuster();

    AtomicBoolean taskRunned = new AtomicBoolean(false);
    lockAcquirer.lockRun(lockRequest, (lockedCluster) -> {
      final StackGresCluster storedCluster = kubeDb
          .getCluster(clusterName, clusterNamespace);
      final Map<String, String> annotations = storedCluster
          .getMetadata().getAnnotations();
      assertNotNull(annotations.get(LOCK_POD_KEY));
      assertEquals(lockRequest.getPodName(), annotations.get(LOCK_POD_KEY));
      assertNotNull(annotations.get(LOCK_TIMESTAMP_KEY));
      assertEquals(lockedCluster, storedCluster);
      taskRunned.set(true);
    });

    assertTrue(taskRunned.get());

  }

  @Test
  void givenAnUnlockedCluster_itShouldReleaseTheLockIfTheTaskExitsSuccessfully() {

    prepareUnlockedCLuster();

    runTaskSuccessfully();

    StackGresCluster lastPatch = kubeDb.getCluster(clusterName, clusterNamespace);
    assertNull(lastPatch.getMetadata().getAnnotations().get(LOCK_POD_KEY));
    assertNull(lastPatch.getMetadata().getAnnotations().get(LOCK_TIMESTAMP_KEY));

  }

  @Test
  void givenALockedClusterByMe_itShouldUpdateTheLockTimestampBeforeRunningTheTask() {

    final long lockTimestamp = (System.currentTimeMillis() / 1000) - 1;
    prepareLockedCluster(lockRequest.getPodName(), lockTimestamp);

    AtomicBoolean taskRunned = new AtomicBoolean(false);

    lockAcquirer.lockRun(lockRequest, (cluster) -> {
      taskRunned.set(true);
      StackGresCluster lastPatch = kubeDb.getCluster(clusterName, clusterNamespace);
      final Map<String, String> annotations = lastPatch.getMetadata().getAnnotations();
      assertNotNull(annotations.get(LOCK_POD_KEY));
      assertNotNull(annotations.get(LOCK_TIMESTAMP_KEY));
      assertTrue(() -> {
        long finalTimestamp = Long.parseLong(annotations.get(LOCK_TIMESTAMP_KEY));
        return finalTimestamp > lockTimestamp;
      });
    });

  }

  @Test
  void givenALockedCluster_itShouldWaitUntilTheLockIsReleasedBeforeRunningTheTask() {

    final long lockTimestamp = (System.currentTimeMillis() / 1000) - 1;
    prepareLockedCluster(lockRequest.getLockResourceName(), lockTimestamp);

    AtomicBoolean taskRan = asycRunTaskSuccessfully();

    sleep(lockRequest.getPollInterval() + 1);

    assertFalse(taskRan.get());

    removeLock();

    sleep(lockRequest.getPollInterval() + 2);

    assertTrue(taskRan.get());

  }

  @Test
  void givenATimedoutLockedCluster_itShouldOverrideTheLock() {

    final long lockTimestamp =
        (System.currentTimeMillis() / 1000) - lockRequest.getTimeout() + 1;
    prepareLockedCluster(lockRequest.getLockResourceName(), lockTimestamp);

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

    long lockTimestamp = Long.parseLong(kubeDb.getCluster(clusterName, clusterNamespace)
        .getMetadata().getAnnotations().get(LOCK_TIMESTAMP_KEY));

    long currentTimestamp = System.currentTimeMillis() / 1000;

    assertTrue(currentTimestamp - lockTimestamp <= lockRequest.getPollInterval());

    sleep(lockRequest.getPollInterval() + 3);

    assertTrue(taskRan.get());

  }

  private void removeLock() {
    var cluster = kubeDb.getCluster(clusterName, clusterNamespace);
    cluster.getMetadata().getAnnotations().remove(LOCK_POD_KEY);
    cluster.getMetadata().getAnnotations().remove(LOCK_TIMESTAMP_KEY);
    kubeDb.addOrReplaceCluster(cluster);
  }

  private void runTaskSuccessfully() {
    AtomicBoolean taskRan = new AtomicBoolean(false);

    lockAcquirer.lockRun(lockRequest, (cluster) -> {
      StackGresCluster lastPatch = kubeDb.getCluster(clusterName, clusterNamespace);
      final Map<String, String> annotations = lastPatch.getMetadata().getAnnotations();
      assertEquals(lockRequest.getPodName(), annotations.get(LOCK_POD_KEY));
      taskRan.set(true);
    });

    assertTrue(taskRan.get());
  }

  private AtomicBoolean asycRunTaskSuccessfully() {
    return asycRunTaskSuccessfully(0);
  }

  private AtomicBoolean asycRunTaskSuccessfully(int delay) {
    AtomicBoolean taskRan = new AtomicBoolean(false);

    executorService.execute(() -> lockAcquirer.lockRun(lockRequest, (cluster) -> {
      if (delay > 0) {
        sleep(delay);
      }
      StackGresCluster lastPatch = kubeDb.getCluster(clusterName, clusterNamespace);
      final Map<String, String> annotations = lastPatch.getMetadata().getAnnotations();
      assertEquals(lockRequest.getPodName(), annotations.get(LOCK_POD_KEY),
          "Task ran without Lock!!");
      assertNotNull(annotations.get(LOCK_TIMESTAMP_KEY));
      taskRan.set(true);
    }));

    return taskRan;
  }

  private void prepareUnlockedCLuster() {
    cluster.setStatus(null);
    final Map<String, String> annotations = cluster.getMetadata().getAnnotations();
    annotations.remove(LOCK_POD_KEY);
    annotations.remove(LOCK_TIMESTAMP_KEY);
    kubeDb.addOrReplaceCluster(cluster);
  }

  private void prepareLockedCluster(String lockPod, Long lockTimestamp) {
    cluster.setStatus(null);
    final Map<String, String> annotations = cluster.getMetadata().getAnnotations();
    annotations.put(LOCK_POD_KEY, lockPod);
    annotations.put(LOCK_TIMESTAMP_KEY, Long.toString(lockTimestamp));
    kubeDb.addOrReplaceCluster(cluster);

  }

  private void sleep(int seconds) {
    try {
      Thread.sleep(seconds * 1000L);
    } catch (InterruptedException ignored) {
      // ignored
    }
  }

}
