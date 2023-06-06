/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import static io.stackgres.common.RetryUtil.calculateExponentialBackoffDelay;

import java.util.function.Supplier;

import io.fabric8.kubernetes.client.KubernetesClientException;

public interface KubernetesClientUtil {

  /**
   * Return true when exception is a conflict (409) error.
   */
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
            throw new RuntimeException(iex);
          }
        }
        throw ex;
      }
    }
  }

}
