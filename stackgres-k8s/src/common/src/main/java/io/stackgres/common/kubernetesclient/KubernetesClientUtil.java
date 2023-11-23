/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import static io.stackgres.common.RetryUtil.calculateExponentialBackoffDelay;

import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.LoggerFactory;

public interface KubernetesClientUtil {

  /**
   * Return true when exception is a conflict (409) error.
   */
  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  static boolean isConflict(Throwable ex) {
    return ex instanceof KubernetesClientException kce
        && kce.getCode() == 409;
  }

  /**
   * Retry on conflict (409) error with back-off.
   */
  static void retryOnConflict(Runnable runnable) {
    retryOnConflict((Supplier<Void>) () -> {
      runnable.run();
      return null;
    });
  }

  /**
   * Retry on conflict (409) error with back-off.
   */
  static <T> T retryOnConflict(Supplier<T> supplier) {
    int retry = 0;
    while (true) {
      try {
        return supplier.get();
      } catch (KubernetesClientException ex) {
        if (isConflict(ex)) {
          try {
            Thread.sleep(calculateExponentialBackoffDelay(10, 600, 10, retry++));
            continue;
          } catch (InterruptedException iex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(iex);
          }
        }
        throw ex;
      }
    }
  }

  /**
   * Retry on error.
   */
  static void retryOnError(Runnable runnable, int maxRetries) {
    retryOnError((Supplier<Void>) () -> {
      runnable.run();
      return null;
    }, maxRetries);
  }

  /**
   * Retry on error.
   */
  static <T> T retryOnError(Supplier<T> supplier, int maxRetries) {
    int retry = 0;
    while (true) {
      try {
        return supplier.get();
      } catch (KubernetesClientException ex) {
        try {
          Thread.sleep(calculateExponentialBackoffDelay(3000, 30000, 1000, retry));
        } catch (InterruptedException iex) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(iex);
        }
        if (retry++ > maxRetries) {
          throw ex;
        }
        LoggerFactory.getLogger(KubernetesClientUtil.class)
            .warn("Retry {} after error: {}", retry, ex.getMessage());
      }
    }
  }

}
