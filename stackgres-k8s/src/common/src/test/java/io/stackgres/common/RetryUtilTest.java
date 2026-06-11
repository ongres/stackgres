/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class RetryUtilTest {

  @Test
  void retryWithLimitShouldPerformExactlyTheConfiguredRetries() {
    AtomicInteger attempts = new AtomicInteger();
    RuntimeException failure = assertThrows(RuntimeException.class,
        () -> RetryUtil.retryWithLimit(
            (Runnable) () -> {
              attempts.incrementAndGet();
              throw new RuntimeException("test");
            },
            ex -> true, 2, 1, 1, 0));
    assertEquals("test", failure.getMessage());
    assertEquals(3, attempts.get());
  }

  @Test
  void retryWithLimitOfZeroShouldNotRetry() {
    AtomicInteger attempts = new AtomicInteger();
    assertThrows(RuntimeException.class,
        () -> RetryUtil.retryWithLimit(
            (Runnable) () -> {
              attempts.incrementAndGet();
              throw new RuntimeException("test");
            },
            ex -> true, 0, 1, 1, 0));
    assertEquals(1, attempts.get());
  }

  @Test
  void exponentialBackoffDelayShouldNeverBeNegative() {
    for (int retry = 0; retry < 10; retry++) {
      for (int iteration = 0; iteration < 100; iteration++) {
        int delay = RetryUtil.calculateExponentialBackoffDelay(10, 600, 1000, retry);
        assertTrue(delay >= 0, "delay " + delay + " is negative");
      }
    }
  }

  @Test
  void exponentialBackoffDelayWithZeroVariationShouldNotThrow() {
    assertEquals(10, RetryUtil.calculateExponentialBackoffDelay(10, 600, 0, 0));
  }

}
