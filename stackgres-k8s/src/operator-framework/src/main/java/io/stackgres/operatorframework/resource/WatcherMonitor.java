/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
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

  @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "Method is called by private inner class")
  private void onEventReceived() {
    retries.set(0);
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
      onEventReceived();
    }

    @Override
    public void watcherClosed(Exception ex) {
      onWatcherClosed(ex);
    }
  }
}
