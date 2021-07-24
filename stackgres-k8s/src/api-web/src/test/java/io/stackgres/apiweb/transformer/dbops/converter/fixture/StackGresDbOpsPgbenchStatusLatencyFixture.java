/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.dbops.converter.fixture;

import java.math.BigDecimal;

import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusLatency;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusMeasure;

public class StackGresDbOpsPgbenchStatusLatencyFixture {

  public StackGresDbOpsPgbenchStatusLatency build() {
    StackGresDbOpsPgbenchStatusLatency latency = new StackGresDbOpsPgbenchStatusLatency();
    latency.setAverage(new StackGresDbOpsPgbenchStatusMeasure(new BigDecimal(10.00), "ms"));
    latency
        .setStandardDeviation(new StackGresDbOpsPgbenchStatusMeasure(new BigDecimal(1.00), "ms"));
    return latency;
  }

}
