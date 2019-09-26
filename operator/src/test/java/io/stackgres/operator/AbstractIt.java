/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jooq.lambda.Unchecked;
import org.jooq.lambda.fi.lang.CheckedRunnable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractIt {

  protected final static Logger LOGGER = LoggerFactory.getLogger(AbstractIt.class);

  protected ExecutorService executor;

  @BeforeEach
  public final void setupServiceExecutor() throws Exception {
    executor = Executors.newCachedThreadPool(runnable -> new Thread(runnable, "it"));
  }

  protected final CompletableFuture<Void> runAsync(CheckedRunnable runnable) {
    return CompletableFuture.runAsync(Unchecked.runnable(runnable), executor);
  }

  @AfterEach
  public final void teardownServiceExecutor() throws Exception {
    executor.shutdown();
    executor.awaitTermination(3, TimeUnit.SECONDS);
  }

}
