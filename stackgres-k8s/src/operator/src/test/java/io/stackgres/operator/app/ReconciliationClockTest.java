/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import io.stackgres.operator.conciliation.AbstractReconciliator;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class ReconciliationClockTest {

  @Test
  @SuppressWarnings("unchecked")
  void resyncsWatchersBeforePeriodicReconciliation() {
    OperatorWatchersHandler operatorWatchersHandler = mock(OperatorWatchersHandler.class);
    Instance<AbstractReconciliator<?>> reconciliators = mock(Instance.class, Mockito.CALLS_REAL_METHODS);
    AbstractReconciliator<?> firstReconciliator = mock(AbstractReconciliator.class);
    AbstractReconciliator<?> secondReconciliator = mock(AbstractReconciliator.class);
    when(reconciliators.iterator())
        .thenReturn(List.<AbstractReconciliator<?>>of(firstReconciliator, secondReconciliator).iterator());

    ReconciliationClock reconciliationClock =
        new ReconciliationClock(operatorWatchersHandler, reconciliators);

    reconciliationClock.reconcile();

    InOrder inOrder = inOrder(
        operatorWatchersHandler,
        firstReconciliator,
        secondReconciliator);
    inOrder.verify(operatorWatchersHandler).resync();
    inOrder.verify(firstReconciliator).reconcileAll();
    inOrder.verify(secondReconciliator).reconcileAll();
    verifyNoMoreInteractions(operatorWatchersHandler, firstReconciliator, secondReconciliator);
  }
}
