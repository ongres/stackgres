/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.quarkus.runtime.ShutdownEvent;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DbOpsExecutorService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbOpsExecutorService.class);

  private ExecutorService executorService;

  public ExecutorService getExecutorService() {
    return executorService;
  }

  public <T> Uni<T> itemAsync(Supplier<T> supplier) {
    return Uni.createFrom().completionStage(
        () -> CompletableFuture.supplyAsync(supplier, executorService));
  }

  public Uni<Void> invokeAsync(Runnable runnable) {
    return Uni.createFrom().completionStage(
        () -> CompletableFuture.runAsync(runnable, executorService));
  }

  @PostConstruct
  void onCreation() {
    this.executorService = Executors.newCachedThreadPool(
        r -> new Thread(r, getClass().getSimpleName()));
    LOGGER.info("Executor service started");
  }

  void onStop(@Observes ShutdownEvent event) {
    executorService.shutdown();
    LOGGER.info("Executor service shutdown started");
    try {
      executorService.awaitTermination(20, TimeUnit.SECONDS);
      LOGGER.info("Executor service shutdown completed");
    } catch (Exception ex) {
      LOGGER.warn("Can not terminate executor service", ex);
    }
  }

}
