/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import static io.stackgres.common.RetryUtil.retry;
import static io.stackgres.common.RetryUtil.retryWithLimit;

import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClientException;

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
    return retry(supplier, KubernetesClientUtil::isConflict, 10, 600, 10);
  }

  /**
   * Retry on error.
   */
  static void retryOnError(Runnable runnable, int retryLimit) {
    retryOnError((Supplier<Void>) () -> {
      runnable.run();
      return null;
    }, retryLimit);
  }

  /**
   * Retry on error.
   */
  static <T> T retryOnError(Supplier<T> supplier, int retryLimit) {
    return retryWithLimit(supplier, ex -> true, retryLimit, 3000, 30000, 1000);
  }

}
