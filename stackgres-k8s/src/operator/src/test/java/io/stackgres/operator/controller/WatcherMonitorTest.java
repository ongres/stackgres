/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.fabric8.kubernetes.client.Watcher;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatcherMonitorTest {

  @Test
  void onMaxRetriesReached_onGiveUpShouldCalled() {

    AtomicBoolean giveUpCalled = new AtomicBoolean(false);

    AtomicReference<WatcherListener<StackGresCluster>> watcherListener = new AtomicReference<>();

    WatcherMonitor<StackGresCluster> monitor = new WatcherMonitor<>(wl -> {
      watcherListener.set(wl);
      return () -> {
      };
    }, () -> giveUpCalled.set(true));

    for (int i = 0; i < WatcherMonitor.MAX_RETRIES; i++){
      watcherListener.get().watcherClosed(new RuntimeException());
    }

    assertTrue(giveUpCalled.get(), "not giving up");
  }

  @Test
  void onMonitorClose_shouldNotCallOnGiveUp() {

    AtomicBoolean giveUpCalled = new AtomicBoolean(false);

    AtomicReference<WatcherListener<StackGresCluster>> watcherListener = new AtomicReference<>();

    WatcherMonitor<StackGresCluster> monitor = new WatcherMonitor<>(wl -> {
      watcherListener.set(wl);
      return () -> {
      };
    }, () -> giveUpCalled.set(true));

    monitor.close();

    assertFalse(giveUpCalled.get(), "give up called on closed");

  }

  @Test
  void receptionOfEvents_shouldResetRetryCounter() {

    AtomicBoolean giveUpCalled = new AtomicBoolean(false);

    AtomicReference<WatcherListener<StackGresCluster>> watcherListener = new AtomicReference<>();

    WatcherMonitor<StackGresCluster> monitor = new WatcherMonitor<>(wl -> {
      watcherListener.set(wl);
      return () -> {
      };
    }, () -> giveUpCalled.set(true));

    for (int i = 0; i < WatcherMonitor.MAX_RETRIES - 1; i++) {
      watcherListener.get().watcherClosed(new RuntimeException());
    }

    watcherListener.get().eventReceived(Watcher.Action.ADDED, null);

    for (int i = 0; i < WatcherMonitor.MAX_RETRIES - 1; i++) {
      watcherListener.get().watcherClosed(new RuntimeException());
    }

    assertFalse(giveUpCalled.get(), "counter not being reset");

  }
}