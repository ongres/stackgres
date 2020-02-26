/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.runtime.Application;
import io.quarkus.runtime.StartupEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InitializationQueue {

  private static final Logger LOGGER = LoggerFactory.getLogger(InitializationQueue.class);

  private ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(1, r -> new Thread(r, "InitializerQueueScheduler"));

  private final List<Runnable> initializers;
  private final AtomicInteger retries = new AtomicInteger(0);
  private final AtomicLong waitTime = new AtomicLong(500);

  @Inject
  public InitializationQueue(
      @Any Instance<DefaultCustomResourceInitializer<?>> initializers) {
    this.initializers = new ArrayList<>(
        initializers.stream()
        .map(initializer -> (Runnable) () -> initializer.initialize())
        .collect(Collectors.toList()));
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.trace("Checking if operator is ready");
    scheduler.schedule(this::initializationCycle, waitTime.get(), TimeUnit.MILLISECONDS);
  }

  protected void initializationCycle() {
    int attempts = retries.addAndGet(1);
    final int size = initializers.size();
    for (int index = 0; index < size; index++) {
      Runnable initializer = initializers.remove(0);
      try {
        initializer.run();
      } catch (Exception ex) {
        LOGGER.error("initialization task failed", ex);
        initializers.add(initializer);
      }
    }
    if (initializers.isEmpty()) {
      scheduler.shutdown();
      return;
    }
    if (attempts >= 5) {
      LOGGER.error("Couldn't complete the initialization phase after 5 attemps.  "
          + "Shutting down...");
      new Thread(() -> Application.currentApplication().stop()).start();
      scheduler.shutdown();
      return;
    }
    long waitTime = this.waitTime.updateAndGet(time -> time * 2);
    scheduler.schedule(this::initializationCycle, waitTime, TimeUnit.MILLISECONDS);
  }

}
