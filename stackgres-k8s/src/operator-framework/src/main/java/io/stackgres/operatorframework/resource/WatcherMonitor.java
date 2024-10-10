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
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatcherMonitor<T extends HasMetadata> implements AutoCloseable {

  private static final int MAX_BACKOFF_SLEEP_SECONDS = 300;

  private static final Logger LOGGER = LoggerFactory.getLogger(WatcherMonitor.class);

  private final MonitorListener listener = new MonitorListener();
  private final Random random = new Random();
  private final String name;
  private final Function<Watcher<T>, Watch> watchCreator;
  private final BiConsumer<Watcher.Action, T> consumer;
  private final Function<Integer, Duration> backoffSleepDuration;
  private final ExecutorService executorService;
  private boolean closed = false;

  private Watch watcher = null;

  public WatcherMonitor(
      String name,
      Function<Watcher<T>, Watch> watchCreator,
      BiConsumer<Watcher.Action, T> consumer) {
    this(name, watchCreator, consumer, null);
  }

  @SuppressWarnings("null")
  public WatcherMonitor(
      String name,
      Function<Watcher<T>, Watch> watcherCreator,
      BiConsumer<Watcher.Action, T> consumer,
      Function<Integer, Duration> backoffSleepDuration) {
    this.name = name;
    this.watchCreator = watcherCreator;
    this.consumer = consumer;
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
        LOGGER.warn("An error occurred while creating watcher {}", name, ex);
        try {
          Thread.sleep(backoffSleepDuration.apply(attempts++).toMillis());
        } catch (InterruptedException iex) {
          break;
        }
      }
    }
  }

  private synchronized void createWatcher() {
    if (!closed) {
      watcher = watchCreator.apply(listener);
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
    closed = true;
    try {
      if (watcher != null) {
        watcher.close();
      }
    } catch (Exception ex) {
      LOGGER.warn("Error while closing watcher {}", name, ex);
    }
  }

  private Duration exponentialBackoffSleepDuration(int attempts) {
    final double pow = Math.pow(2, attempts);
    final int rand = random.nextInt(Math.abs((int) pow));
    return Duration.ofSeconds((long) Math.min(pow + rand, MAX_BACKOFF_SLEEP_SECONDS));
  }

  private class MonitorListener implements Watcher<T> {

    @Override
    public void eventReceived(Watcher.Action action, T resource) {
      try {
        LOGGER.trace("Action <{}> on resource: {} {}.{}", action, resource.getKind(),
            resource.getMetadata().getNamespace(), resource.getMetadata().getName());
        consumer.accept(action, resource);
      } catch (Exception ex) {
        LOGGER.error("Error while performing action <{}> on resource: {} {}.{}", action, resource.getKind(),
            resource.getMetadata().getNamespace(), resource.getMetadata().getName(), ex);
      }
    }

    @Override
    public void onClose() {
      LOGGER.debug("Watcher closed");
    }

    @Override
    public void onClose(WatcherException cause) {
      if (cause.isHttpGone()) {
        LOGGER.warn("An error occurred in watcher {}: {}", name, cause.getMessage());
      } else {
        LOGGER.warn("An error occurred in watcher {}", name, cause);
      }
      onWatcherClosed();
    }

  }
}
