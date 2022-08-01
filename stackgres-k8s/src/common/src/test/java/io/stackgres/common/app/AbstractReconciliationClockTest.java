/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.app;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AbstractReconciliationClockTest {

  @Mock
  Runnable reconciliation;
  AbstractReconciliationClock reconciliationClock;

  @BeforeEach
  public void beforeEach() {
    reconciliationClock = new AbstractReconciliationClock() {
      @Override
      protected void reconcile() {
        reconciliation.run();
      }

      @Override
      protected int getPeriod() {
        return 1;
      }

      @Override
      protected TimeUnit getTimeUnit() {
        return TimeUnit.MILLISECONDS;
      }
    };
  }

  @Test
  public void testReconciliationIsRescheduled() throws Exception {
    reconciliationClock.start();
    verify(reconciliation, timeout(1000).atLeast(2)).run();
    reconciliationClock.stop();
  }

  @Test
  public void testReconciliationIsRescheduledAfterException() throws Exception {
    doThrow(new RuntimeException()).when(reconciliation).run();
    reconciliationClock.start();
    verify(reconciliation, timeout(1000).atLeast(2)).run();
    reconciliationClock.stop();
  }

}
