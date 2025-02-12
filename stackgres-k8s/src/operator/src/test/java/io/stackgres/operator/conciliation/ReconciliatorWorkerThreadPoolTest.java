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
            new ReconciliationRunnable(executor, runnable, "5", true, 0L),
            new ReconciliationRunnable(executor, runnable, "6", true, 20L),
            new ReconciliationRunnable(executor, runnable, "4", false, 10L),
            new ReconciliationRunnable(executor, runnable, "3", false, 20L),
            new ReconciliationRunnable(executor, runnable, "2", false, 50L),
            new ReconciliationRunnable(executor, runnable, "1", false, 100L),
        }),
        Stream.of(new ReconciliationRunnable[] {
            new ReconciliationRunnable(executor, runnable, "1", false, 100L),
            new ReconciliationRunnable(executor, runnable, "2", false, 50L),
            new ReconciliationRunnable(executor, runnable, "3", false, 20L),
            new ReconciliationRunnable(executor, runnable, "4", false, 10L),
            new ReconciliationRunnable(executor, runnable, "5", true, 0L),
            new ReconciliationRunnable(executor, runnable, "6", true, 20L),
        })
        .sorted()
        .toList());
  }

  @Test
  public void checkReconciliationRunnableNaturalOrderWithTimeout() {
    Mockito.when(executor.getPriorityTimeout()).thenReturn(1000L);
    assertEquals(
        List.of(new ReconciliationRunnable[] {
            new ReconciliationRunnable(executor, runnable, "-1", true, 5L),
            new ReconciliationRunnable(executor, runnable, "0", false, 0L),
            new ReconciliationRunnable(executor, runnable, "5", true, 2000L),
            new ReconciliationRunnable(executor, runnable, "6", true, 2020L),
            new ReconciliationRunnable(executor, runnable, "4", false, 2010L),
            new ReconciliationRunnable(executor, runnable, "3", false, 2020L),
            new ReconciliationRunnable(executor, runnable, "2", false, 2050L),
            new ReconciliationRunnable(executor, runnable, "1", false, 2100L),
        }),
        Stream.of(new ReconciliationRunnable[] {
            new ReconciliationRunnable(executor, runnable, "-1", true, 5L),
            new ReconciliationRunnable(executor, runnable, "0", false, 0L),
            new ReconciliationRunnable(executor, runnable, "1", false, 2100L),
            new ReconciliationRunnable(executor, runnable, "2", false, 2050L),
            new ReconciliationRunnable(executor, runnable, "3", false, 2020L),
            new ReconciliationRunnable(executor, runnable, "4", false, 2010L),
            new ReconciliationRunnable(executor, runnable, "5", true, 2000L),
            new ReconciliationRunnable(executor, runnable, "6", true, 2020L),
        })
        .sorted()
        .toList());
  }

}
