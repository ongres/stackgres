/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.security.SecureRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

public interface RetryUtil {

  static int calculateExponentialBackoffDelay(int initial, int maximum, int variation, int retry) {
    return (int) Math.min(maximum, initial * Math.pow(Math.E, retry))
        + (variation - new SecureRandom().nextInt(2 * variation));
  }

  /**
   * Retry with back-off.
   */
  static <E extends RuntimeException> void retry(
      Runnable runnable,
      Predicate<E> predicate,
      int initial, int maximum, int variation) {
    retry((Supplier<Void>) () -> {
      runnable.run();
      return null;
    }, predicate, initial, maximum, variation);
  }

  /**
   * Retry with back-off.
   */
  @SuppressWarnings("unchecked")
  static <T, E extends RuntimeException> T retry(
      Supplier<T> supplier,
      Predicate<E> predicate,
      int initial, int maximum, int variation) {
    int retry = 0;
    while (true) {
      try {
        return supplier.get();
      } catch (RuntimeException ex) {
        if (predicate.test((E) ex)) {
          try {
            int delay = calculateExponentialBackoffDelay(initial, maximum, variation, retry++);
            Thread.sleep(delay);
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
   * Retry with back-off.
   */
  static <E extends RuntimeException> void retryWithLimit(
      Runnable runnable,
      Predicate<E> predicate,
      Integer retryLimit,
      int initial, int maximum, int variation) {
    retryWithLimit((Supplier<Void>) () -> {
      runnable.run();
      return null;
    }, predicate, retryLimit, initial, maximum, variation);
  }

  /**
   * Retry with back-off.
   */
  @SuppressWarnings("unchecked")
  static <T, E extends RuntimeException> T retryWithLimit(
      Supplier<T> supplier,
      Predicate<E> predicate,
      Integer retryLimit,
      int initial, int maximum, int variation) {
    int retry = 0;
    while (true) {
      try {
        return supplier.get();
      } catch (RuntimeException ex) {
        if (predicate.test((E) ex)) {
          try {
            if (retryLimit != null && retryLimit.intValue() < retry) {
              throw ex;
            }
            int delay = calculateExponentialBackoffDelay(initial, maximum, variation, retry++);
            LoggerFactory.getLogger(RetryUtil.class)
                .warn("Will retry after {} milliseconds due to error: {}", delay, ex.getMessage());
            Thread.sleep(delay);
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

}
