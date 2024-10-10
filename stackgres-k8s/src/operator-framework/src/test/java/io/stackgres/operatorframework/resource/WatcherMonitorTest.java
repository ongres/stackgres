/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.google.common.util.concurrent.Uninterruptibles;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WatcherMonitorTest {

  private static final RuntimeException NOP = new RuntimeException("nop");

  BlockingQueue<RuntimeException> initExceptionsToThrowOrNop;

  List<Watcher<Pod>> watchers;
  List<Exception> exceptions;

  @BeforeEach
  public void beforeEach() {
    initExceptionsToThrowOrNop = new ArrayBlockingQueue<>(2);
    watchers = new ArrayList<>();
    exceptions = new ArrayList<>();
  }

  @Test
  void afterWatcherMonitorCreation_watcherShouldBeCreated() throws Exception {
    try (WatcherMonitor<Pod> ignored = createWatcherMonitor()) {
      initExceptionsToThrowOrNop.add(NOP);
      checkWithTimeout(() -> watchers.size() > 0);
    }
  }

  @Test
  void afterWatcherThrowsException_watcherShouldBeRecreated() throws Exception {
    CompletableFuture<Void> sendExceptionToWatcher = new CompletableFuture<>();
    CompletableFuture<Void> closeFinished = new CompletableFuture<>();
    CompletableFuture<Void> closeWatcher = CompletableFuture.runAsync(() -> {
      sendExceptionToWatcher.join();
      watchers.get(0).onClose(new WatcherException("test"));
      closeFinished.complete(null);
    });
    try (WatcherMonitor<Pod> ignored = createWatcherMonitor()) {
      initExceptionsToThrowOrNop.add(NOP);
      checkWithTimeout(() -> watchers.size() > 0);
      sendExceptionToWatcher.complete(null);
      initExceptionsToThrowOrNop.add(NOP);
      checkWithTimeout(() -> watchers.size() > 1);
      closeFinished.join();
    }
    closeWatcher.get(1, TimeUnit.SECONDS);
    Assertions.assertTrue(watchers.size() == 2);
    Assertions.assertTrue(exceptions.size() == 0);
  }

  @Test
  void afterWatcherThrowsExceptionAndWatcherCreationThrowsException_watcherShouldBeRecreated()
      throws Exception {
    CompletableFuture<Void> sendExceptionToWatcher = new CompletableFuture<>();
    CompletableFuture<Void> closeFinished = new CompletableFuture<>();
    CompletableFuture<Void> closeWatcher = CompletableFuture.runAsync(() -> {
      sendExceptionToWatcher.join();
      watchers.get(0).onClose(new WatcherException("test"));
      closeFinished.complete(null);
    });
    try (WatcherMonitor<Pod> ignored = createWatcherMonitor()) {
      initExceptionsToThrowOrNop.add(NOP);
      checkWithTimeout(() -> watchers.size() > 0);
      initExceptionsToThrowOrNop.add(new RuntimeException("test"));
      sendExceptionToWatcher.complete(null);
      checkWithTimeout(() -> exceptions.size() > 0);
      initExceptionsToThrowOrNop.add(NOP);
      checkWithTimeout(() -> watchers.size() > 1);
      closeFinished.join();
    }
    closeWatcher.get(1, TimeUnit.SECONDS);
    Assertions.assertTrue(watchers.size() == 2);
    Assertions.assertTrue(exceptions.size() == 1);
  }

  @Test
  void whenWatcherMonitorCloseOnCreation_watcherShouldNotBeCreated() throws Exception {
    try (WatcherMonitor<Pod> ignored = createWatcherMonitor()) {
      Assertions.assertTrue(true);
    }
    Assertions.assertTrue(watchers.size() == 0);
  }

  @Test
  void whenWatcherMonitorCloseOnRecreation_watcherShouldNotBeRecreated() throws Exception {
    try (WatcherMonitor<Pod> ignored = createWatcherMonitor()) {
      initExceptionsToThrowOrNop.add(NOP);
      checkWithTimeout(() -> watchers.size() > 0);
      initExceptionsToThrowOrNop.add(new RuntimeException("test"));
    }
    Assertions.assertTrue(watchers.size() == 1);
    Assertions.assertTrue(exceptions.size() == 0);
  }

  private WatcherMonitor<Pod> createWatcherMonitor() {
    return new WatcherMonitor<>("test", watcher -> {
      RuntimeException initExceptionToThrowOrNop =
          Unchecked.supplier(() -> initExceptionsToThrowOrNop.take()).get();
      if (initExceptionToThrowOrNop != NOP) {
        exceptions.add(initExceptionToThrowOrNop);
        throw initExceptionToThrowOrNop;
      }
      watchers.add(watcher);
      Watch watch = () -> {
      };
      return watch;
    },
        (a, r) -> {},
        attempts -> Duration.ofSeconds(0));
  }

  private void checkWithTimeout(Supplier<Boolean> predicate)
      throws InterruptedException, ExecutionException, TimeoutException {
    CompletableFuture.runAsync(() -> {
      while (!predicate.get()) {
        Uninterruptibles.sleepUninterruptibly(Duration.ofMillis(10));
      }
    }).get(1, TimeUnit.SECONDS);
  }

}
