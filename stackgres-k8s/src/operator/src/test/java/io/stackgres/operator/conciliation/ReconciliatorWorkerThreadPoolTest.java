/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import io.stackgres.operator.conciliation.ReconciliatorWorkerThreadPool.ReconciliationRunnable;
import io.stackgres.operator.conciliation.ReconciliatorWorkerThreadPool.ReconciliatorThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliatorWorkerThreadPoolTest {

  @Mock
  private ReconciliatorThreadPoolExecutor executor;

  private Runnable runnable = () -> {};

  @Test
  public void checkReconciliationRunnableNaturalOrder() {
    assertEquals(
        List.of(new ReconciliationRunnable[] {
            new ReconciliationRunnable(executor, runnable, "5", 1, 0L),
            new ReconciliationRunnable(executor, runnable, "6", 1, 20L),
            new ReconciliationRunnable(executor, runnable, "4", 0, 10L),
            new ReconciliationRunnable(executor, runnable, "3", 0, 20L),
            new ReconciliationRunnable(executor, runnable, "2", 0, 50L),
            new ReconciliationRunnable(executor, runnable, "1", 0, 100L),
        }),
        Stream.of(new ReconciliationRunnable[] {
            new ReconciliationRunnable(executor, runnable, "1", 0, 100L),
            new ReconciliationRunnable(executor, runnable, "2", 0, 50L),
            new ReconciliationRunnable(executor, runnable, "3", 0, 20L),
            new ReconciliationRunnable(executor, runnable, "4", 0, 10L),
            new ReconciliationRunnable(executor, runnable, "5", 1, 0L),
            new ReconciliationRunnable(executor, runnable, "6", 1, 20L),
        })
        .sorted()
        .toList());
  }

  @Test
  public void checkReconciliationRunnableNaturalOrderWithTimeout() {
    Mockito.when(executor.getPriorityTimeout()).thenReturn(1000L);
    assertEquals(
        List.of(new ReconciliationRunnable[] {
            new ReconciliationRunnable(executor, runnable, "-1", 1, 5L),
            new ReconciliationRunnable(executor, runnable, "0", 0, 0L),
            new ReconciliationRunnable(executor, runnable, "5", 1, 2000L),
            new ReconciliationRunnable(executor, runnable, "6", 1, 2020L),
            new ReconciliationRunnable(executor, runnable, "4", 0, 2010L),
            new ReconciliationRunnable(executor, runnable, "3", 0, 2020L),
            new ReconciliationRunnable(executor, runnable, "2", 0, 2050L),
            new ReconciliationRunnable(executor, runnable, "1", 0, 2100L),
        }),
        Stream.of(new ReconciliationRunnable[] {
            new ReconciliationRunnable(executor, runnable, "-1", 1, 5L),
            new ReconciliationRunnable(executor, runnable, "0", 0, 0L),
            new ReconciliationRunnable(executor, runnable, "1", 0, 2100L),
            new ReconciliationRunnable(executor, runnable, "2", 0, 2050L),
            new ReconciliationRunnable(executor, runnable, "3", 0, 2020L),
            new ReconciliationRunnable(executor, runnable, "4", 0, 2010L),
            new ReconciliationRunnable(executor, runnable, "5", 1, 2000L),
            new ReconciliationRunnable(executor, runnable, "6", 1, 2020L),
        })
        .sorted()
        .toList());
  }

}
