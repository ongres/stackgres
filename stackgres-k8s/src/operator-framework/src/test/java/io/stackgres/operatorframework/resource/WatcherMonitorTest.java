/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.WatcherException;
import org.junit.jupiter.api.Test;

class WatcherMonitorTest {

  @Test
  void onMaxRetriesReached_onGiveUpShouldCalled() {

    AtomicBoolean giveUpCalled = new AtomicBoolean(false);

    Deque<RuntimeException> exceptionsToThrow = new ArrayDeque<>();

    AtomicReference<WatcherListener<Pod>> watcherListener = new AtomicReference<>();

    try (WatcherMonitor<Pod> ignored = new WatcherMonitor<>(wl -> {
      RuntimeException exceptionToThrow = exceptionsToThrow.poll();
      if (exceptionToThrow != null) {
        throw exceptionToThrow;
      }
      watcherListener.set(wl);
      return () -> {
      };
    }, () -> giveUpCalled.set(true))) {
      for (int i = 0; i < WatcherMonitor.MAX_RETRIES - 1; i++){
        exceptionsToThrow.add(new RuntimeException());
      }
      watcherListener.get().watcherError(new WatcherException(""));

      assertTrue(giveUpCalled.get(), "not giving up");
    }
  }

  @Test
  void onMonitorClose_shouldNotCallOnGiveUp() {

    AtomicBoolean giveUpCalled = new AtomicBoolean(false);

    Deque<RuntimeException> exceptionsToThrow = new ArrayDeque<>();

    AtomicReference<WatcherListener<Pod>> watcherListener = new AtomicReference<>();

    WatcherMonitor<Pod> monitor = new WatcherMonitor<>(wl -> {
      RuntimeException exceptionToThrow = exceptionsToThrow.poll();
      if (exceptionToThrow != null) {
        throw exceptionToThrow;
      }
      watcherListener.set(wl);
      return () -> {
      };
    }, () -> giveUpCalled.set(true));

    monitor.close();

    assertFalse(giveUpCalled.get(), "give up called on closed");

  }

  @Test
  void receptionOfError_shouldResetRetryCounter() {

    AtomicBoolean giveUpCalled = new AtomicBoolean(false);

    Deque<RuntimeException> exceptionsToThrow = new ArrayDeque<>();

    AtomicReference<WatcherListener<Pod>> watcherListener = new AtomicReference<>();

    try(WatcherMonitor<Pod> monitor = new WatcherMonitor<>(wl -> {
      RuntimeException exceptionToThrow = exceptionsToThrow.poll();
      if (exceptionToThrow != null) {
        throw exceptionToThrow;
      }
      watcherListener.set(wl);
      return () -> {
      };
    }, () -> giveUpCalled.set(true))) {
      for (int i = 0; i < WatcherMonitor.MAX_RETRIES - 2; i++){
        exceptionsToThrow.add(new RuntimeException());
      }
      watcherListener.get().watcherError(new WatcherException(""));

      for (int i = 0; i < WatcherMonitor.MAX_RETRIES - 2; i++){
        exceptionsToThrow.add(new RuntimeException());
      }
      watcherListener.get().watcherError(new WatcherException(""));

      assertFalse(giveUpCalled.get(), "counter not being reset");
    }
  }
}