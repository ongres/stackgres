/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
  private final Function<Integer, Duration> backoffSleepDuration;
  private final ExecutorService executorService;
  private boolean closeCalled = false;

  private Watch watcher = null;

  public WatcherMonitor(
      String name,
      Function<WatcherListener<T>, Watch> watcherCreator) {
    this(name, watcherCreator, null);
  }

  @SuppressWarnings("null")
  public WatcherMonitor(
      String name,
      Function<WatcherListener<T>, Watch> watcherCreator,
      Function<Integer, Duration> backoffSleepDuration) {
    this.name = name;
    this.watcherCreator = watcherCreator;
    this.backoffSleepDuration = Optional.ofNullable(backoffSleepDuration)
        .orElse(this::exponentialBackoffSleepDuration);
    this.executorService = Executors.newFixedThreadPool(
        1, r -> new Thread(r, "WatcherInit-" + name));
    this.executorService.execute(this::run);
  }

  private void run() {
    tryCreateWatcher();
    this.executorService.shutdown();
  }

  private void onWatcherClosed() {
    tryCreateWatcher();
  }

  private void tryCreateWatcher() {
    int attempts = 1;
    while (true) {
      try {
        createWatcher();
        break;
      } catch (Exception ex) {
        LOGGER.warn("An error occurred while creating watcher " + name, ex);
        try {
          Thread.sleep(backoffSleepDuration.apply(attempts++).toMillis());
        } catch (InterruptedException iex) {
          break;
        }
      }
    }
  }

  private synchronized void createWatcher() {
    if (!closeCalled) {
      watcher = watcherCreator.apply(listener);
    }
  }

  @Override
  public void close() {
    closeWatcher();
    try {
      this.executorService.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException iex) {
      return;
    }
  }

  private synchronized void closeWatcher() {
    closeCalled = true;
    try {
      if (watcher != null) {
        watcher.close();
      }
    } catch (Exception ex) {
      LOGGER.warn("Error while closing watcher " + name, ex);
    }
  }

  private Duration exponentialBackoffSleepDuration(int attempts) {
    final double pow = Math.pow(2, attempts);
    final int rand = random.nextInt(Math.abs((int) pow));
    return Duration.ofSeconds((long) Math.min(pow + rand, MAX_BACKOFF_SLEEP_SECONDS));
  }

  private class MonitorListener implements WatcherListener<T> {
    @Override
    public void eventReceived(Watcher.Action action, T resource) {
    }

    @Override
    public void watcherError(WatcherException ex) {
      LOGGER.warn("An error occurred in watcher " + name, ex);
      onWatcherClosed();
    }

    @Override
    public void watcherClosed() {
    }
  }
}
