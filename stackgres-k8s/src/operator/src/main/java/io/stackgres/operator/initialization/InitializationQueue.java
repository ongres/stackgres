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
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClientException;
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
    scheduler.schedule(this::initializationCycle, 500, TimeUnit.MILLISECONDS);
  }

  protected void initializationCycle() {
    int attempts = retries.addAndGet(1);
    final int size = initializers.size();
    for (int index = 0; index < size; index++) {
      Runnable initializer = initializers.remove(0);
      try {
        initializer.run();
      } catch (Exception ex) {
        if (ex instanceof KubernetesClientException
            && ex.getMessage().contains("connect: connection refused")) {
          LOGGER.debug("Initialization task failed: {}", ex.getMessage());
        } else {
          LOGGER.warn("Initialization task failed", ex);
        }
        initializers.add(initializer);
      }
    }
    if (initializers.isEmpty()) {
      scheduler.shutdown();
      return;
    }
    if (attempts >= 30) {
      LOGGER.error("Couldn't complete the initialization phase after 30 attemps.  "
          + "Shutting down...");
      new Thread(() -> Application.currentApplication().stop()).start();
      scheduler.shutdown();
      return;
    }
    scheduler.schedule(this::initializationCycle, 500, TimeUnit.MILLISECONDS);
  }

}
