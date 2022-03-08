/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackGresDbOpsPgbenchStatusTest {

  private static final double TPS_EXCLUDING_CONN = 200.98d;
  private static final String TPS_MEASURE_UNIT = "tps";
  private static final String LATENCY_UNIT = "ms";
  private static final double TPS_INCLUDING_CONN = 100.89d;
  private static final double LATENCY_STD_DEV = 10.02d;
  private static final double LATENCY_AVERAGE_VALUE = 10.01d;
  private StackGresDbOpsPgbenchStatus status;

  @BeforeEach
  void setup() {
    this.status = Fixtures.dbOps().loadPgbench().get()
        .getStatus().getBenchmark().getPgbench();
  }

  @Test
  void shouldStackGresPgbenchStatus_hasLatencyAverage() {
    StackGresDbOpsPgbenchStatusMeasure latencyAverage = status.getLatency().getAverage();
    assertEquals(LATENCY_AVERAGE_VALUE, latencyAverage.getValue().doubleValue());
    assertEquals(LATENCY_UNIT, latencyAverage.getUnit());
  }

  @Test
  void shouldStackGresPgbenchStatus_hasLatencyStandartDeviation() {
    StackGresDbOpsPgbenchStatusMeasure latencyStdDev = status.getLatency().getStandardDeviation();
    assertEquals(LATENCY_STD_DEV, latencyStdDev.getValue().doubleValue());
    assertEquals(LATENCY_UNIT, latencyStdDev.getUnit());
  }

  @Test
  void shouldStackGresPgbenchStatus_hasTpsIncludingConnections() {
    StackGresDbOpsPgbenchStatusMeasure tpsIncludingConn = status.getTransactionsPerSecond()
        .getIncludingConnectionsEstablishing();
    assertEquals(TPS_INCLUDING_CONN, tpsIncludingConn.getValue().doubleValue());
    assertEquals(TPS_MEASURE_UNIT, tpsIncludingConn.getUnit());
  }

  @Test
  void shouldStackGresPgbenchStatus_hasTpsExcludingConnections() {
    StackGresDbOpsPgbenchStatusMeasure tpsExcludingConn = status.getTransactionsPerSecond()
        .getExcludingConnectionsEstablishing();
    assertEquals(TPS_EXCLUDING_CONN, tpsExcludingConn.getValue().doubleValue());
    assertEquals(TPS_MEASURE_UNIT, tpsExcludingConn.getUnit());
  }

}
