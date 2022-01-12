/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import java.util.Random;
import java.util.function.Supplier;

import io.fabric8.kubernetes.client.KubernetesClientException;

public interface KubernetesClientUtil {

  /**
   * Return true when exception is a conflict (409) error.
   */
  static boolean isConflict(Throwable ex) {
    return ex instanceof KubernetesClientException
        && KubernetesClientException.class.cast(ex).getCode() == 409;
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
            Thread.sleep(
                (int) Math.min(3000, 10 * Math.pow(Math.E, retry++))
                + (10 - new Random().nextInt(20)));
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
