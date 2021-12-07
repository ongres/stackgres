/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.mutiny;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

public interface UniUtil {

  static <T> T waitUniIndefinitely(Uni<T> uni) {
    return waitUni(uni, Optional.empty());
  }

  static <T> T waitUni(Uni<T> uni, Optional<Duration> timeout) {
    CompletableFuture<T> completableFuture = new CompletableFuture<>();
    uni
        .subscribe()
        .with(completableFuture::complete, completableFuture::completeExceptionally);
    try {
      if (timeout.isPresent()) {
        return completableFuture.get(timeout.get().getSeconds(), TimeUnit.SECONDS);
      } else {
        return completableFuture.get();
      }
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    } catch (java.util.concurrent.TimeoutException timeoutEx) {
      throw new TimeoutException();
    } catch (ExecutionException ex) {
      final Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new RuntimeException(cause);
    }
  }

}
