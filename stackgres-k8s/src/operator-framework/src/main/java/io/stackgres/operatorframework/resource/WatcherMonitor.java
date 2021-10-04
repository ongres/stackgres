/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import com.google.common.util.concurrent.Uninterruptibles;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatcherMonitor<T> implements AutoCloseable {

  private static final int MAX_BACKOFF_SLEEP_SECONDS = 60_000;

  private static final Logger LOGGER = LoggerFactory.getLogger(WatcherMonitor.class);

  private final MonitorListener listener = new MonitorListener();
  private final Random random = new Random();
  private final String name;
  private final Function<WatcherListener<T>, Watch> watcherCreator;
  private final ExecutorService executorService;
  private boolean closeCalled = false;

  private Watch watcher = null;

  public WatcherMonitor(
      String name,
      Function<WatcherListener<T>, Watch> watcherCreator) {
    this.name = name;
    this.watcherCreator = watcherCreator;
    this.executorService = Executors.newFixedThreadPool(1, r -> new Thread(r, "Watcher-" + name));
    this.executorService.execute(this::run);
  }

  private void run() {
    int attempt = 1;
    while (true) {
      try {
        watcher = watcherCreator.apply(listener);
        break;
      } catch (Exception ex) {
        LOGGER.warn("An error occurred while creating watcher " + name, ex);
        Uninterruptibles.sleepUninterruptibly(exponentialBackoffSleepDuration(attempt++));
      }
    }
    this.executorService.shutdown();
  }

  private synchronized void onWatcherClosed(Exception cause) {
    if (closeCalled) {
      return;
    }
    int attempts = 1;
    try {
      watcher = watcherCreator.apply(listener);
    } catch (Exception ex) {
      LOGGER.warn("An error occurred while creating watcher " + name, ex);
      Uninterruptibles.sleepUninterruptibly(exponentialBackoffSleepDuration(attempts++));
      onWatcherClosed(ex);
    }
  }

  @Override
  public synchronized void close() {
    closeCalled = true;
    try {
      watcher.close();
    } catch (Exception ex) {
      LOGGER.warn("Error while closing watcher " + name, ex);
    }
  }

  private Duration exponentialBackoffSleepDuration(int attempts) {
    final double pow = Math.pow(2, attempts);
    final int rand = random.nextInt(1000);
    return Duration.ofSeconds((long) Math.min(pow + rand, MAX_BACKOFF_SLEEP_SECONDS));
  }

  private class MonitorListener implements WatcherListener<T> {
    @Override
    public void eventReceived(Watcher.Action action, T resource) {
    }

    @Override
    public void watcherError(WatcherException ex) {
      LOGGER.warn("An error occurred in watcher " + name, ex);
      onWatcherClosed(ex);
    }

    @Override
    public void watcherClosed() {
    }
  }
}
