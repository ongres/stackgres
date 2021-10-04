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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.google.common.util.concurrent.Uninterruptibles;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.WatcherException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WatcherMonitorTest {

  private static final RuntimeException NOP = new RuntimeException("nop");

  BlockingQueue<RuntimeException> initExceptionsToThrowOrNop;

  AtomicReference<WatcherListener<Pod>> watcherListener;

  List<Watch> watchs;

  @BeforeEach
  public void beforeEach() {
    initExceptionsToThrowOrNop = new ArrayBlockingQueue<>(2);
    watcherListener = new AtomicReference<>();
    watchs = new ArrayList<>();
  }

  @Test
  void afterWatcherMonitorCreation_watcherShouldBeCreated() throws Exception {
    try (WatcherMonitor<Pod> ignored = createWatcherMonitor()) {
      initExceptionsToThrowOrNop.add(NOP);
      checkWithTimeout(() -> watchs.size() >= 1);
    }
  }

  @Test
  void afterWatcherThrowsException_watcherShouldBeRecreated() throws Exception {
    try (WatcherMonitor<Pod> ignored = createWatcherMonitor()) {
      initExceptionsToThrowOrNop.add(NOP);
      checkWithTimeout(() -> watchs.size() >= 1);
      initExceptionsToThrowOrNop.add(NOP);
      watcherListener.get().watcherError(new WatcherException("test"));
      checkWithTimeout(() -> watchs.size() >= 2);
    }
  }

  @Test
  void afterWatcherThrowsExceptionAndWatcherCreationThrowsException_watcherShouldBeRecreated()
      throws Exception {
    try (WatcherMonitor<Pod> ignored = createWatcherMonitor()) {
      initExceptionsToThrowOrNop.add(NOP);
      checkWithTimeout(() -> watchs.size() >= 1);
      initExceptionsToThrowOrNop.add(new RuntimeException("test"));
      initExceptionsToThrowOrNop.add(NOP);
      watcherListener.get().watcherError(new WatcherException("test"));
      checkWithTimeout(() -> watchs.size() >= 2);
    }
  }

  @Test
  void whenWatcherMonitorCloseOnCreation_watcherShouldNotBeCreated() throws Exception {
    try (WatcherMonitor<Pod> ignored = createWatcherMonitor()) {
      Assertions.assertTrue(true);
    }
    Assertions.assertTrue(watchs.size() == 0);
  }

  @Test
  void whenWatcherMonitorCloseOnRecreation_watcherShouldNotBeRecreated() throws Exception {
    final CompletableFuture<Void> throwWatcherError;
    try (WatcherMonitor<Pod> ignored = createWatcherMonitor()) {
      initExceptionsToThrowOrNop.add(NOP);
      checkWithTimeout(() -> watchs.size() >= 1);
      initExceptionsToThrowOrNop.add(new RuntimeException("test"));
      throwWatcherError = CompletableFuture.runAsync(
          () -> watcherListener.get().watcherError(new WatcherException("test")));
    }
    throwWatcherError.join();
    Assertions.assertTrue(watchs.size() == 1);
  }

  private WatcherMonitor<Pod> createWatcherMonitor() {
    return new WatcherMonitor<>("test", wl -> {
      RuntimeException initExceptionToThrowOrNop =
          Uninterruptibles.takeUninterruptibly(initExceptionsToThrowOrNop);
      if (initExceptionToThrowOrNop != NOP) {
        throw initExceptionToThrowOrNop;
      }
      watcherListener.set(wl);
      Watch watch = () -> {
      };
      watchs.add(watch);
      return watch;
    }, attempts -> Duration.ofSeconds(0));
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
