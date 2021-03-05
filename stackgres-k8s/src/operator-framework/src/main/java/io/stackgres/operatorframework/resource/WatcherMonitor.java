/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatcherMonitor<T> implements AutoCloseable {

  public static final int MAX_RETRIES = 5;
  private static final Logger LOGGER = LoggerFactory.getLogger(WatcherMonitor.class);
  private final AtomicInteger retries = new AtomicInteger(0);
  private final MonitorListener listener = new MonitorListener();

  private final Function<WatcherListener<T>, Watch> watcherCreator;
  private final Runnable giveUp;
  private boolean closeCalled = false;

  private Watch watcher;

  public WatcherMonitor(
      Function<WatcherListener<T>, Watch> watcherCreator,
      Runnable giveUp) {
    this.watcherCreator = watcherCreator;
    watcher = watcherCreator.apply(listener);
    this.giveUp = giveUp;
  }

  private void onWatcherClosed(Exception cause) {
    if (closeCalled) {
      return;
    }
    int currentRetries = retries.addAndGet(1);
    if (currentRetries >= MAX_RETRIES) {
      LOGGER.error("Giving up on retrying watcher", cause);
      giveUp.run();
    } else {
      try {
        watcher = watcherCreator.apply(listener);
        retries.set(0);
      } catch (Exception ex) {
        ex.addSuppressed(cause);
        onWatcherClosed(ex);
      }
    }
  }

  @Override
  public void close() {
    closeCalled = true;
    watcher.close();
  }

  private class MonitorListener implements WatcherListener<T> {
    @Override
    public void eventReceived(Watcher.Action action, T resource) {
    }

    @Override
    public void watcherError(WatcherException ex) {
      onWatcherClosed(ex);
    }

    @Override
    public void watcherClosed() {
    }
  }
}
